<%@ page contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Login - Digital Library</title>

    <link rel="stylesheet" href="style.css">
</head>

<body>

<div class="box">

    <h2><center>Digital Library</center></h2>

    <%
        String flash = (String) session.getAttribute("flash");

        if (flash != null) {
            session.removeAttribute("flash");
    %>

        <p class="message">
            <%= flash %>
        </p>

    <%
        }
    %>

    <form method="post" action="app">

        <input type="hidden"
               name="action"
               value="login">

        <label>Email</label>

        <input type="email"
               name="email"
               placeholder="Enter your email"
               required>


        <label>Password</label>

        <input type="password"
               name="password"
               placeholder="Enter your password"
               required>


        <button type="submit">
            Login
        </button>

    </form>


    <p>
        New user?
        <a href="register.jsp">Register</a>
    </p>


    <p class="hint">
        Admin: admin@library.com / admin123
    </p>

</div>

</body>
</html>