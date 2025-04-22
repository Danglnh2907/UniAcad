<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Upload Excel File</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .form-group {
            margin-bottom: 15px;
        }
        .message {
            margin-top: 20px;
            padding: 10px;
            border-radius: 5px;
        }
        .success {
            background-color: #d4edda;
            color: #155724;
        }
        .error {
            background-color: #f8d7da;
            color: #721c24;
        }
    </style>
</head>
<body>
<h2>Upload Excel File to Register Students</h2>
<p>Excel file should have: Column 1 (Email), Column 2 (Photo)</p>
<form action="UploadServlet" method="post" enctype="multipart/form-data">
    <div class="form-group">
        <label for="file">Choose Excel File (.xlsx):</label>
        <input type="file" id="file" name="file" accept=".xlsx" required />
    </div>
    <input type="submit" value="Upload" />
</form>
<% String message = (String) request.getAttribute("message"); %>
<% if (message != null) { %>
<div class="message <%= message.startsWith("Error") ? "error" : "success" %>">
    <%= message %>
</div>
<% } %>
</body>
</html>