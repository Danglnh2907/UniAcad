
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.util.Map, util.service.excel.ExcelService.ProcessingError" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Excel File Upload and Display</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
    <style>
        .table-container {
            max-height: 400px;
            overflow-y: auto;
        }
        img {
            max-width: 100px;
            height: auto;
        }
    </style>
</head>
<body class="bg-gray-100 flex flex-col items-center justify-center min-h-screen">
<div class="bg-white p-6 rounded-lg shadow-lg w-full max-w-4xl">
    <h1 class="text-2xl font-bold mb-4 text-center">Upload and Display Excel Data</h1>

    <!-- File Upload Form -->
    <form action="uploadExcel" method="post" enctype="multipart/form-data" class="mb-6">
        <div class="flex items-center justify-center">
            <input type="file" name="excelFile" accept=".xlsx" class="border p-2 rounded mr-2" required>
            <button type="submit" class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">Upload</button>
        </div>
    </form>

    <!-- Error Message -->
    <%
        String errorMessage = (String) request.getAttribute("errorMessage");
        if (errorMessage != null && !errorMessage.isEmpty()) {
    %>
    <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
        <%= errorMessage %>
    </div>
    <% } %>

    <!-- Excel Data Display -->
    <%
        List<Map<String, Object>> excelData = (List<Map<String, Object>>) request.getAttribute("excelData");
        String fileName = (String) request.getAttribute("fileName");
        if (excelData != null && !excelData.isEmpty()) {
    %>
    <h2 class="text-xl font-semibold mb-2">Data from: <%= fileName %></h2>
    <div class="table-container">
        <table class="min-w-full bg-white border">
            <thead>
            <tr class="bg-gray-200">
                <%
                    for (String key : excelData.get(0).keySet()) {
                %>
                <th class="border px-4 py-2"><%= key %></th>
                <% } %>
            </tr>
            </thead>
            <tbody>
            <%
                for (Map<String, Object> row : excelData) {
            %>
            <tr>
                <%
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                %>
                <td class="border px-4 py-2">
                    <%
                        if (value != null && key.equals("Image") &&
                                (value.toString().endsWith(".png") || value.toString().endsWith(".jpg"))) {
                    %>
                    <img src="<%= request.getContextPath() %>/resources/img/<%= value %>" alt="Image">
                    <%
                    } else {
                    %>
                    <%= value != null ? value.toString() : "" %>
                    <% } %>
                </td>
                <% } %>
            </tr>
            <% } %>
            </tbody>
        </table>
    </div>
    <% } %>

    <!-- Processing Errors -->
    <%
        List<ProcessingError> errors = (List<ProcessingError>) request.getAttribute("errors");
        if (errors != null && !errors.isEmpty()) {
    %>
    <h2 class="text-xl font-semibold mt-6 mb-2 text-red-600">Processing Errors</h2>
    <div class="table-container">
        <table class="min-w-full bg-white border">
            <thead>
            <tr class="bg-red-200">
                <th class="border px-4 py-2">Row</th>
                <th class="border px-4 py-2">Column</th>
                <th class="border px-4 py-2">Error Message</th>
                <th class="border px-4 py-2">Error Type</th>
            </tr>
            </thead>
            <tbody>
            <%
                for (ProcessingError error : errors) {
            %>
            <tr>
                <td class="border px-4 py-2"><%= error.getRowIndex() %></td>
                <td class="border px-4 py-2"><%= error.getColumnIndex() %></td>
                <td class="border px-4 py-2"><%= error.getErrorMessage() %></td>
                <td class="border px-4 py-2"><%= error.getErrorType() %></td>
            </tr>
            <% } %>
            </tbody>
        </table>
    </div>
    <% } %>
</div>
</body>
</html>
