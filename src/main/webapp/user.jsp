<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.util.*,lib.DB"%>
<%!
String btn(String a, Object id, String label) {
  return "<form class='inl' method='post' action='app'><input type='hidden' name='action' value='"+a+"'><input type='hidden' name='id' value='"+id+"'><button class='sm'>"+label+"</button></form>";
}
%>
<%
Map<String,Object> me=(Map<String,Object>)session.getAttribute("user");
if(me==null){response.sendRedirect("login.jsp");return;}
List<Map<String,Object>> books=(List<Map<String,Object>>)request.getAttribute("books");
if(books==null){response.sendRedirect("app");return;}
List<Map<String,Object>> cats=(List<Map<String,Object>>)request.getAttribute("cats");
List<Map<String,Object>> mine=(List<Map<String,Object>>)request.getAttribute("myIssues");
List<Map<String,Object>> bk=(List<Map<String,Object>>)request.getAttribute("myBookings");
String q=request.getParameter("q")==null?"":request.getParameter("q");
String cat=request.getParameter("cat")==null?"":request.getParameter("cat");
int due=0; for(Map<String,Object> r:mine){ if(((Number)r.get("fine_paid")).intValue()==0) due+=((Number)r.get("fine")).intValue(); }
String f=(String)session.getAttribute("flash"); if(f!=null) session.removeAttribute("flash");
%>
<!DOCTYPE html><html><head><title>Library</title><link rel="stylesheet" href="style.css"></head><body>
<div class="nav"><b>Welcome, <%=DB.h(me.get("name"))%></b><span><a href="#catalogue">Catalogue</a><a href="#mine">My Books</a><a href="#contact">Contact</a><a href="app?action=logout">Logout</a></span></div>
<div class="wrap">
<% if(f!=null){ %><p class="flash"><%=DB.h(f)%></p><% } %>
<% if(due>0){ %><p class="flash">Outstanding fines: Rs.<%=due%> (pay at the library desk; admin marks it paid)</p><% } %>

<h2 id="catalogue">Browse &amp; Search Books</h2>
<form method="get" action="app" class="row">
<input name="q" placeholder="Title or author" value="<%=DB.h(q)%>">
<select name="cat"><option value="">All categories</option>
<% for(Map<String,Object> c:cats){ String cn=(String)c.get("category"); %><option value="<%=DB.h(cn)%>" <%= cn.equals(cat)?"selected":"" %>><%=DB.h(cn)%></option><% } %></select>
<button>Search</button></form>
<table><tr><th>Title</th><th>Author</th><th>Category</th><th>Available</th><th>Action</th></tr>
<% for(Map<String,Object> b:books){ int av=((Number)b.get("available")).intValue(); %>
<tr><td><%=DB.h(b.get("title"))%></td><td><%=DB.h(b.get("author"))%></td><td><%=DB.h(b.get("category"))%></td><td><%=av%> / <%=b.get("quantity")%></td>
<td><%= av>0 ? btn("issue",b.get("id"),"Issue") : btn("book",b.get("id"),"Book in advance") %></td></tr><% } %></table>

<h2 id="mine">My Issued Books</h2>
<table><tr><th>Book</th><th>Issued</th><th>Due</th><th>Returned</th><th>Fine (Rs.)</th><th>Action</th></tr>
<% for(Map<String,Object> r:mine){ boolean paid=((Number)r.get("fine_paid")).intValue()==1; %>
<tr><td><%=DB.h(r.get("title"))%></td><td><%=r.get("issue_date")%></td><td><%=r.get("due_date")%></td><td><%= r.get("return_date")==null?"-":r.get("return_date") %></td>
<td><%=r.get("fine")%><%= paid?" (paid)":"" %></td><td><%= r.get("return_date")==null? btn("return",r.get("id"),"Return") : "" %></td></tr><% } %></table>

<h2>My Advance Bookings</h2>
<table><tr><th>Book</th><th>Date</th><th>Status</th></tr>
<% for(Map<String,Object> k:bk){ %><tr><td><%=DB.h(k.get("title"))%></td><td><%=k.get("booking_date")%></td><td><%=k.get("status")%><%= "READY".equals(k.get("status"))?" - you can issue it now":"" %></td></tr><% } %></table>

<h2 id="contact">Contact / Query Form</h2>
<form method="post" action="app" class="col"><input type="hidden" name="action" value="contact">
<input name="subject" placeholder="Subject" required><textarea name="message" rows="4" placeholder="Your message" required></textarea><button>Send</button></form>
</div></body></html>