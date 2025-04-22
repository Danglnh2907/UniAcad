<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Login</title>
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
    <h2 class="text-2xl font-bold text-center text-gray-800 mb-6">Login to Your Account</h2>
    <% if (request.getAttribute("error") != null) { %>
    <div id="error-toast" class="toast bg-red-500 text-white p-4 rounded-lg mb-4">
        <%= request.getAttribute("error") %>
    </div>
    <% } %>
    <form id="login-form" action="login" method="post" class="space-y-4">
        <div>
            <label for="email" class="block text-sm font-medium text-gray-700">Email</label>
            <input type="email" id="email" name="email" required
                   class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                   oninput="validateEmail()">
            <p id="email-error" class="text-red-500 text-sm mt-1 hidden">Invalid email format</p>
        </div>
        <div>
            <label for="password" class="block text-sm font-medium text-gray-700">Password</label>
            <input type="password" id="password" name="password" required
                   class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                   oninput="validatePassword()">
            <p id="password-error" class="text-red-500 text-sm mt-1 hidden">Password must be at least 6 characters</p>
        </div>
        <button type="submit" id="submit-btn"
                class="w-full bg-primary text-white font-semibold py-2 rounded-lg hover:bg-blue-700 transition duration-300 flex items-center justify-center">
            <span id="btn-text">Login</span>
            <svg id="spinner" class="hidden h-5 w-5 animate-spin text-white ml-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
        </button>
    </form>
    <div class="mt-4 text-center">
        <p class="text-gray-600 mb-2">Or login with:</p>
        <a href="google-auth" class="inline-flex items-center bg-secondary text-white font-semibold py-2 px-4 rounded-lg hover:bg-red-500 transition duration-300">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12.545,10.239v3.821h5.445c-0.712,2.315-2.647,3.972-5.445,3.972c-3.332,0-6.033-2.701-6.033-6.032s2.701-6.032,6.033-6.032c1.498,0,2.866,0.549,3.921,1.453l2.814-2.814C17.503,2.988,15.139,2,12.545,2C7.021,2,2.543,6.477,2.543,12s4.478,10,10.002,10c8.396,0,10.249-7.507,9.518-11.426H12.545z"/>
            </svg>
            Google
        </a>
    </div>
    <p class="mt-4 text-center text-gray-600">Don't have an account? <a href="register.jsp" class="text-primary hover:underline">Register</a></p>
</div>
<script>
    function validateEmail() {
        const email = document.getElementById('email').value;
        const emailError = document.getElementById('email-error');
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            emailError.classList.remove('hidden');
            return false;
        } else {
            emailError.classList.add('hidden');
            return true;
        }
    }

    function validatePassword() {
        const password = document.getElementById('password').value;
        const passwordError = document.getElementById('password-error');
        if (password.length < 6) {
            passwordError.classList.remove('hidden');
            return false;
        } else {
            passwordError.classList.add('hidden');
            return true;
        }
    }

    document.getElementById('login-form').addEventListener('submit', function (e) {
        if (!validateEmail() || !validatePassword()) {
            e.preventDefault();
            showToast('Please fix the errors in the form.', 'error');
            return;
        }
        const btn = document.getElementById('submit-btn');
        const btnText = document.getElementById('btn-text');
        const spinner = document.getElementById('spinner');
        btn.disabled = true;
        btnText.textContent = 'Logging in...';
        spinner.classList.remove('hidden');
    });

    function showToast(message, type) {
        const toast = document.createElement('div');
        toast.className = `toast fixed bottom-4 right-4 p-4 rounded-lg text-white ${type == 'error' ? 'bg-red-500' : 'bg-green-500'}`;
        toast.textContent = message;
        document.body.appendChild(toast);
        setTimeout(() => toast.classList.add('show'), 100);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 500);
        }, 3000);
    }

    if (document.getElementById('error-toast')) {
        document.getElementById('error-toast').classList.add('show');
        setTimeout(() => {
            document.getElementById('error-toast').classList.remove('show');
        }, 3000);
    }
</script>
</body>
</html>