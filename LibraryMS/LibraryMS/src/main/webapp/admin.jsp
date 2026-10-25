<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.util.*,lib.DB"%>
<%!
String btn(String a, Object id, String label) {
  return "<form class='inl' method='post' action='app' onsubmit=\"return confirm('Are you sure?')\"><input type='hidden' name='action' value='"+a+"'><input type='hidden' name='id' value='"+id+"'><button class='sm'>"+label+"</button></form>";
}
%>
<%
Map<String,Object> me=(Map<String,Object>)session.getAttribute("user");
if(me==null||!"ADMIN".equals(me.get("role"))){response.sendRedirect("login.jsp");return;}
List<Map<String,Object>> books=(List<Map<String,Object>>)request.getAttribute("books");
if(books==null){response.sendRedirect("app");return;}
List<Map<String,Object>> issues=(List<Map<String,Object>>)request.getAttribute("issues");
List<Map<String,Object>> members=(List<Map<String,Object>>)request.getAttribute("members");
List<Map<String,Object>> msgs=(List<Map<String,Object>>)request.getAttribute("messages");
List<Map<String,Object>> bookings=(List<Map<String,Object>>)request.getAttribute("bookings");
Map<String,Object> eb=(Map<String,Object>)request.getAttribute("editBook");
String f=(String)session.getAttribute("flash"); if(f!=null) session.removeAttribute("flash");
%>
<!DOCTYPE html><html><head><title>Admin</title><link rel="stylesheet" href="style.css"></head><body>
<div class="nav"><b>Library Admin</b><span><a href="#books">Books</a><a href="#issued">Issued &amp; Fines</a><a href="#members">Members</a><a href="#bookings">Bookings</a><a href="#msgs">Queries</a><a href="app?action=logout">Logout</a></span></div>
<div class="wrap">
<% if(f!=null){ %><p class="flash"><%=DB.h(f)%></p><% } %>

<h2 id="books"><%= eb==null?"Add New Book":"Edit Book" %></h2>
<form method="post" action="app" class="row">
<input type="hidden" name="action" value="<%= eb==null?"addBook":"updateBook" %>">
<% if(eb!=null){ %><input type="hidden" name="id" value="<%=eb.get("id")%>"><% } %>
<input name="title" placeholder="Title" required value="<%= eb==null?"":DB.h(eb.get("title")) %>">
<input name="author" placeholder="Author" required value="<%= eb==null?"":DB.h(eb.get("author")) %>">
<input name="isbn" placeholder="ISBN" required value="<%= eb==null?"":DB.h(eb.get("isbn")) %>">
<input name="category" placeholder="Category" required value="<%= eb==null?"":DB.h(eb.get("category")) %>">
<input name="quantity" type="number" min="0" placeholder="Qty" required value="<%= eb==null?"":eb.get("quantity") %>">
<button><%= eb==null?"Add":"Save" %></button><% if(eb!=null){ %><a href="app">Cancel</a><% } %>
</form>

<h2>All Books</h2>
<form method="get" action="app" class="row"><input name="q" placeholder="Search title/author/ISBN" value="<%=DB.h(request.getParameter("q"))%>"><button>Search</button></form>
<table><tr><th>ID</th><th>Title</th><th>Author</th><th>ISBN</th><th>Category</th><th>Qty</th><th>Available</th><th>Actions</th></tr>
<% for(Map<String,Object> b:books){ %><tr><td><%=b.get("id")%></td><td><%=DB.h(b.get("title"))%></td><td><%=DB.h(b.get("author"))%></td><td><%=DB.h(b.get("isbn"))%></td><td><%=DB.h(b.get("category"))%></td><td><%=b.get("quantity")%></td><td><%=b.get("available")%></td>
<td><a href="app?editId=<%=b.get("id")%>#books">Edit</a> <%=btn("deleteBook",b.get("id"),"Delete")%></td></tr><% } %></table>

<h2 id="issued">Issued Books &amp; Fines</h2>
<table><tr><th>Member</th><th>Book</th><th>Issued</th><th>Due</th><th>Returned</th><th>Fine (Rs.)</th><th>Fine status</th></tr>
<% for(Map<String,Object> r:issues){ int fine=((Number)r.get("fine")).intValue(); boolean paid=((Number)r.get("fine_paid")).intValue()==1; %>
<tr><td><%=DB.h(r.get("name"))%></td><td><%=DB.h(r.get("title"))%></td><td><%=r.get("issue_date")%></td><td><%=r.get("due_date")%></td><td><%= r.get("return_date")==null?"Not returned":r.get("return_date") %></td><td><%=fine%></td>
<td><% if(fine==0){ %>-<% } else if(paid){ %>Paid<% } else if(r.get("return_date")==null){ %>Accruing<% } else { %>Unpaid <%=btn("payFine",r.get("id"),"Mark Paid")%><% } %></td></tr><% } %></table>

<h2 id="members">Members</h2>
<table><tr><th>ID</th><th>Name</th><th>Email</th><th>Status</th><th>Action</th></tr>
<% for(Map<String,Object> m:members){ boolean act=((Number)m.get("active")).intValue()==1; %>
<tr><td><%=m.get("id")%></td><td><%=DB.h(m.get("name"))%></td><td><%=DB.h(m.get("email"))%></td><td><%= act?"Active":"Disabled" %></td><td><%=btn("toggleMember",m.get("id"),act?"Disable":"Enable")%></td></tr><% } %></table>

<h2 id="bookings">Advance Bookings</h2>
<table><tr><th>Member</th><th>Book</th><th>Date</th><th>Status</th></tr>
<% for(Map<String,Object> k:bookings){ %><tr><td><%=DB.h(k.get("name"))%></td><td><%=DB.h(k.get("title"))%></td><td><%=k.get("booking_date")%></td><td><%=k.get("status")%></td></tr><% } %></table>

<h2 id="msgs">User Queries</h2>
<table><tr><th>From</th><th>Subject</th><th>Message</th><th>Date</th><th></th></tr>
<% for(Map<String,Object> m:msgs){ %><tr><td><%=DB.h(m.get("name"))%> (<%=DB.h(m.get("email"))%>)</td><td><%=DB.h(m.get("subject"))%></td><td><%=DB.h(m.get("message"))%></td><td><%=m.get("created")%></td><td><%=btn("deleteMessage",m.get("id"),"Delete")%></td></tr><% } %></table>
</div></body></html>