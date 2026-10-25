<%@ page contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Register - Digital Library</title>

    <link rel="stylesheet" href="style.css">
</head>

<body>

<div class="box">

    <h2>Create Account</h2>

    <form method="post" action="app">

        <input type="hidden"
               name="action"
               value="register">


        <label>Name</label>

        <input type="text"
               name="name"
               placeholder="Enter your name"
               required>


        <label>Email</label>

        <input type="email"
               name="email"
               placeholder="Enter your email"
               required>


        <label>Password</label>

        <input type="password"
               name="password"
               placeholder="Create a password"
               required>


        <button type="submit">
            Register
        </button>

    </form>


    <p>
        Already have an account?
        <a href="login.jsp">Login</a>
    </p>

</div>

</body>
</html>