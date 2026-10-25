package lib;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = "/app", loadOnStartup = 1)
public class LibraryServlet extends HttpServlet {
	static final int LOAN_DAYS = 14, FINE_PER_DAY = 5;

	@Override
	public void init() {
		DB.init();
	}

	@Override
	protected void doGet(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException {
		handle(rq, rs);
	}

	@Override
	protected void doPost(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException {
		handle(rq, rs);
	}

	static String p(HttpServletRequest rq, String k) {
		String v = rq.getParameter(k);
		return v == null ? "" : v.trim();
	}

	static int i(HttpServletRequest rq, String k) {
		try {
			return Integer.parseInt(p(rq, k));
		} catch (Exception e) {
			return 0;
		}
	}

	static int n(Object o) {
		return o == null ? 0 : ((Number) o).intValue();
	}

	@SuppressWarnings("unchecked")
	void handle(HttpServletRequest rq, HttpServletResponse rs) throws IOException, ServletException {
		rq.setCharacterEncoding("UTF-8");
		String a = p(rq, "action"), base = rq.getContextPath();
		HttpSession s = rq.getSession();
		Map<String, Object> u = (Map<String, Object>) s.getAttribute("user");
		try {
			if (a.equals("login")) {
				List<Map<String, Object>> r = DB.query("SELECT * FROM users WHERE email=? AND password=? AND active=1",
						p(rq, "email").toLowerCase(), DB.hash(p(rq, "password")));
				if (r.isEmpty()) {
					s.setAttribute("flash", "Invalid credentials or account disabled.");
					rs.sendRedirect(base + "/login.jsp");
				} else {
					s.setAttribute("user", r.get(0));
					rs.sendRedirect(base + "/app");
				}
				return;
			}
			if (a.equals("register")) {
				String name = p(rq, "name"), email = p(rq, "email").toLowerCase(), pw = p(rq, "password");
				if (name.isEmpty() || email.isEmpty() || pw.length() < 4)
					s.setAttribute("flash", "Fill all fields (password min 4 chars).");
				else if (!DB.query("SELECT id FROM users WHERE email=?", email).isEmpty())
					s.setAttribute("flash", "Email already registered.");
				else {
					DB.update("INSERT INTO users(name,email,password,role) VALUES(?,?,?,'USER')", name, email,
							DB.hash(pw));
					s.setAttribute("flash", "Registered! Please login.");
					rs.sendRedirect(base + "/login.jsp");
					return;
				}
				rs.sendRedirect(base + "/register.jsp");
				return;
			}
			if (a.equals("logout")) {
				s.invalidate();
				rs.sendRedirect(base + "/login.jsp");
				return;
			}
			if (u == null) {
				rs.sendRedirect(base + "/login.jsp");
				return;
			}

			boolean admin = "ADMIN".equals(u.get("role"));
			int uid = n(u.get("id"));
			if (!a.isEmpty() && !a.equals("home")) {
				String msg = admin ? adminAction(a, rq) : userAction(a, rq, uid);
				if (msg != null)
					s.setAttribute("flash", msg);
				rs.sendRedirect(base + "/app");
				return;
			}

			// ---- load data for pages ----
			String q = p(rq, "q"), cat = p(rq, "cat"), like = "%" + q + "%";
			rq.setAttribute("books", DB.query(
					"SELECT * FROM books WHERE (title LIKE ? OR author LIKE ? OR isbn LIKE ?) AND (?='' OR category=?) ORDER BY title",
					like, like, like, cat, cat));
			rq.setAttribute("cats", DB.query("SELECT DISTINCT category FROM books ORDER BY category"));
			if (admin) {
				List<Map<String, Object>> issues = DB.query(
						"SELECT i.*, b.title, u.name FROM issues i JOIN books b ON b.id=i.book_id JOIN users u ON u.id=i.user_id ORDER BY i.id DESC");
				liveFines(issues);
				rq.setAttribute("issues", issues);
				rq.setAttribute("members", DB.query("SELECT * FROM users WHERE role='USER' ORDER BY id"));
				rq.setAttribute("messages", DB.query(
						"SELECT m.*, u.name, u.email FROM messages m JOIN users u ON u.id=m.user_id ORDER BY m.id DESC"));
				rq.setAttribute("bookings", DB.query(
						"SELECT k.*, b.title, u.name FROM bookings k JOIN books b ON b.id=k.book_id JOIN users u ON u.id=k.user_id WHERE k.status<>'FULFILLED' ORDER BY k.id"));
				if (i(rq, "editId") > 0) {
					List<Map<String, Object>> e = DB.query("SELECT * FROM books WHERE id=?", i(rq, "editId"));
					if (!e.isEmpty())
						rq.setAttribute("editBook", e.get(0));
				}
				rq.getRequestDispatcher("/admin.jsp").forward(rq, rs);
			} else {
				List<Map<String, Object>> mine = DB.query(
						"SELECT i.*, b.title FROM issues i JOIN books b ON b.id=i.book_id WHERE i.user_id=? ORDER BY i.id DESC",
						uid);
				liveFines(mine);
				rq.setAttribute("myIssues", mine);
				rq.setAttribute("myBookings", DB.query(
						"SELECT k.*, b.title FROM bookings k JOIN books b ON b.id=k.book_id WHERE k.user_id=? ORDER BY k.id DESC",
						uid));
				rq.getRequestDispatcher("/user.jsp").forward(rq, rs);
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/** For books not yet returned and overdue, show running fine */
	static void liveFines(List<Map<String, Object>> rows) {
		LocalDate today = LocalDate.now();
		for (Map<String, Object> r : rows) {
			if (r.get("return_date") == null) {
				long late = ChronoUnit.DAYS.between(LocalDate.parse((String) r.get("due_date")), today);
				r.put("fine", late > 0 ? (int) late * FINE_PER_DAY : 0);
			}
		}
	}

	String userAction(String a, HttpServletRequest rq, int uid) throws Exception {
		LocalDate today = LocalDate.now();
		int bid = i(rq, "id");
		switch (a) {
		case "issue": {
			List<Map<String, Object>> b = DB.query("SELECT * FROM books WHERE id=?", bid);
			if (b.isEmpty())
				return "Book not found.";
			if (n(b.get(0).get("available")) <= 0)
				return "No copies available. You can book it in advance.";
			if (!DB.query("SELECT id FROM issues WHERE book_id=? AND user_id=? AND return_date IS NULL", bid, uid)
					.isEmpty())
				return "You already have this book.";
			DB.update("UPDATE books SET available=available-1 WHERE id=?", bid);
			DB.update("INSERT INTO issues(book_id,user_id,issue_date,due_date) VALUES(?,?,?,?)", bid, uid,
					today.toString(), today.plusDays(LOAN_DAYS).toString());
			DB.update("UPDATE bookings SET status='FULFILLED' WHERE book_id=? AND user_id=? AND status<>'FULFILLED'",
					bid, uid);
			return "Book issued. Due date: " + today.plusDays(LOAN_DAYS);
		}
		case "return": {
			List<Map<String, Object>> r = DB
					.query("SELECT * FROM issues WHERE id=? AND user_id=? AND return_date IS NULL", bid, uid);
			if (r.isEmpty())
				return "Issue record not found.";
			int book = n(r.get(0).get("book_id"));
			long late = ChronoUnit.DAYS.between(LocalDate.parse((String) r.get(0).get("due_date")), today);
			int fine = late > 0 ? (int) late * FINE_PER_DAY : 0;
			DB.update("UPDATE issues SET return_date=?, fine=? WHERE id=?", today.toString(), fine, bid);
			DB.update("UPDATE books SET available=available+1 WHERE id=?", book);
			DB.update("UPDATE bookings SET status='READY' WHERE book_id=? AND status='PENDING' ORDER BY id LIMIT 1",
					book);

			return "Book returned." + (fine > 0 ? " Overdue fine: Rs." + fine : "");
		}
		case "book": {
			List<Map<String, Object>> b = DB.query("SELECT * FROM books WHERE id=?", bid);
			if (b.isEmpty())
				return "Book not found.";
			if (n(b.get(0).get("available")) > 0)
				return "Copies are available - issue it directly.";
			if (!DB.query("SELECT id FROM bookings WHERE book_id=? AND user_id=? AND status<>'FULFILLED'", bid, uid)
					.isEmpty())
				return "You already booked this.";
			DB.update("INSERT INTO bookings(book_id,user_id,booking_date) VALUES(?,?,?)", bid, uid, today.toString());
			return "Advance booking done. Status becomes READY when a copy is returned.";
		}
		case "contact":
			DB.update("INSERT INTO messages(user_id,subject,message,created) VALUES(?,?,?,?)", uid, p(rq, "subject"),
					p(rq, "message"), today.toString());
			return "Message sent to admin.";
		}
		return null;
	}

	String adminAction(String a, HttpServletRequest rq) throws Exception {
		int id = i(rq, "id");
		switch (a) {
		case "addBook": {
			int qty = Math.max(i(rq, "quantity"), 0);
			DB.update("INSERT INTO books(title,author,isbn,category,quantity,available) VALUES(?,?,?,?,?,?)",
					p(rq, "title"), p(rq, "author"), p(rq, "isbn"), p(rq, "category"), qty, qty);
			return "Book added.";
		}
		case "updateBook": {
			List<Map<String, Object>> o = DB.query("SELECT * FROM books WHERE id=?", id);
			if (o.isEmpty())
				return "Book not found.";
			int qty = Math.max(i(rq, "quantity"), 0);
			int avail = n(o.get(0).get("available")) + (qty - n(o.get(0).get("quantity")));
			if (avail < 0)
				return "Quantity cannot be lower than copies currently issued.";
			DB.update("UPDATE books SET title=?,author=?,isbn=?,category=?,quantity=?,available=? WHERE id=?",
					p(rq, "title"), p(rq, "author"), p(rq, "isbn"), p(rq, "category"), qty, avail, id);
			return "Book updated.";
		}
		case "deleteBook": {
			if (!DB.query("SELECT id FROM issues WHERE book_id=? AND return_date IS NULL", id).isEmpty())
				return "Cannot delete: copies are currently issued.";
			DB.update("DELETE FROM bookings WHERE book_id=?", id);
			DB.update("DELETE FROM issues WHERE book_id=?", id);
			DB.update("DELETE FROM books WHERE id=?", id);
			return "Book deleted.";
		}
		case "toggleMember":
			DB.update("UPDATE users SET active=1-active WHERE id=? AND role='USER'", id);
			return "Member status changed.";
		case "payFine":
			DB.update("UPDATE issues SET fine_paid=1 WHERE id=? AND fine>0", id);
			return "Fine marked as paid.";
		case "deleteMessage":
			DB.update("DELETE FROM messages WHERE id=?", id);
			return "Message deleted.";
		}
		return null;
	}
}
