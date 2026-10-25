package lib;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DB {
	// ====== CHANGE THESE TO MATCH YOUR MYSQL ======
	static final String DB_NAME = "librarydb";
	static final String USER = "root";
	static final String PASS = "tiger";
	// ===============================================

	static final String URL = "jdbc:mysql://localhost:3306/" + DB_NAME
			+ "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
			+ "&serverTimezone=UTC&characterEncoding=UTF-8";

	public static Connection get() throws SQLException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver"); // Connector/J 8.x
		} catch (ClassNotFoundException e) {
			try {
				Class.forName("com.mysql.jdbc.Driver"); // Connector/J 5.x
			} catch (ClassNotFoundException e2) {
				throw new SQLException("MySQL JDBC driver jar not found", e2);
			}
		}
		return DriverManager.getConnection(URL, USER, PASS);
	}

	public static List<Map<String, Object>> query(String sql, Object... p) throws SQLException {
		List<Map<String, Object>> out = new ArrayList<>();
		try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
			for (int i = 0; i < p.length; i++)
				ps.setObject(i + 1, p[i]);
			try (ResultSet rs = ps.executeQuery()) {
				ResultSetMetaData m = rs.getMetaData();
				while (rs.next()) {
					Map<String, Object> row = new HashMap<>();
					for (int i = 1; i <= m.getColumnCount(); i++)
						row.put(m.getColumnLabel(i).toLowerCase(), rs.getObject(i));
					out.add(row);
				}
			}
		}
		return out;
	}

	public static int update(String sql, Object... p) throws SQLException {
		try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
			for (int i = 0; i < p.length; i++)
				ps.setObject(i + 1, p[i]);
			return ps.executeUpdate();
		}
	}

	public static void init() {
		try {
			update("CREATE TABLE IF NOT EXISTS users("
					+ "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), email VARCHAR(100) UNIQUE, "
					+ "password VARCHAR(64), role VARCHAR(10) DEFAULT 'USER', active INT DEFAULT 1"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
			update("CREATE TABLE IF NOT EXISTS books("
					+ "id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(200), author VARCHAR(150), "
					+ "isbn VARCHAR(30), category VARCHAR(80), quantity INT, available INT"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
			update("CREATE TABLE IF NOT EXISTS issues("
					+ "id INT AUTO_INCREMENT PRIMARY KEY, book_id INT, user_id INT, "
					+ "issue_date VARCHAR(10), due_date VARCHAR(10), return_date VARCHAR(10), "
					+ "fine INT DEFAULT 0, fine_paid INT DEFAULT 0" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
			update("CREATE TABLE IF NOT EXISTS bookings("
					+ "id INT AUTO_INCREMENT PRIMARY KEY, book_id INT, user_id INT, "
					+ "booking_date VARCHAR(10), status VARCHAR(15) DEFAULT 'PENDING'"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
			update("CREATE TABLE IF NOT EXISTS messages("
					+ "id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, subject VARCHAR(200), "
					+ "message TEXT, created VARCHAR(10)" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

			if (query("SELECT id FROM users WHERE role='ADMIN'").isEmpty())
				update("INSERT INTO users(name,email,password,role) VALUES('Admin','admin@library.com',?,'ADMIN')",
						hash("admin123"));
			if (query("SELECT id FROM books").isEmpty()) {
				update("INSERT INTO books(title,author,isbn,category,quantity,available) VALUES('Effective Java','Joshua Bloch','9780134685991','Programming',3,3)");
				update("INSERT INTO books(title,author,isbn,category,quantity,available) VALUES('Clean Code','Robert Martin','9780132350884','Programming',2,2)");
				update("INSERT INTO books(title,author,isbn,category,quantity,available) VALUES('Wings of Fire','A.P.J. Abdul Kalam','9788173711466','Biography',1,1)");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static String hash(String s) {
		try {
			StringBuilder sb = new StringBuilder();
			for (byte b : MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8")))
				sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** HTML-escape for safe output in JSP */
	public static String h(Object o) {
		if (o == null)
			return "";
		return o.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}