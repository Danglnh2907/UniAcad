<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Account</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
  <script src="https://cdn.tailwindcss.com"></script>
  <script>
    tailwind.config = {
      theme: {
        extend: {
          fontFamily: {
            sans: ['Inter', 'sans-serif'],
          },
          colors: {
            primary: '#1E40AF',
            secondary: '#EF4444',
          },
        },
      },
    };
  </script>
  <style>
    .toast { opacity: 0; transition: opacity 0.5s; }
    .toast.show { opacity: 1; }
  </style>
</head>
<body class="bg-gradient-to-br from-blue-100 to-white min-h-screen flex items-center justify-center">
<div class="max-w-md w-full mx-4 p-8 bg-white rounded-xl shadow-lg">
  <h2 class="text-2xl font-bold text-center text-gray-800 mb-6">Account Information</h2>
  <% if (session.getAttribute("email") == null) { %>
  <div class="bg-red-500 text-white p-4 rounded-lg mb-4">
    Please login to view this page.
  </div>
  <a href="login.jsp" class="block text-center bg-primary text-white font-semibold py-2 rounded-lg hover:bg-blue-700 transition duration-300">Login</a>
  <% } else { %>
  <div class="space-y-4">
    <p><strong class="text-gray-700">Email:</strong> <%= session.getAttribute("email") %></p>
    <p><strong class="text-gray-700">Full Name:</strong> <%= session.getAttribute("full_name") != null ? session.getAttribute("full_name") : "Not provided" %></p>
    <a href="logout" class="block text-center bg-secondary text-white font-semibold py-2 rounded-lg hover:bg-red-500 transition duration-300">Logout</a>
  </div>
  <% } %>
</div>
</body>
</html>