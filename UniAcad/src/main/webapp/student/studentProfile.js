async function loadStudentProfile() {
    try {
        const response = await fetch(window.location.origin + '/UniAcad/api/student/profile');
        const result = await response.json();

        if (result.error === 0) {
            const student = result.data;
            document.getElementById('profileInfo').innerHTML = `
                <p><strong>Student ID:</strong> ${student.studentID}</p>
                <p><strong>Name:</strong> ${student.studentName}</p>
                <p><strong>Email:</strong> ${student.studentEmail}</p>
                <p><strong>Phone:</strong> ${student.studentPhone}</p>
                <p><strong>DoB:</strong> ${student.studentDoB}</p>
                <p><strong>Gender:</strong> ${student.studentGender == 0 ? 'Male' : 'Female'}</p>
                <p><strong>Address:</strong> ${student.address || 'N/A'}</p>
                <p><strong>Status:</strong> ${renderStudentStatus(student.studentStatus)}</p>
            `;
        } else {
            document.getElementById('profileInfo').innerHTML = `<p style="color:red;">${result.message}</p>`;
        }
    } catch (error) {
        console.error('Error loading profile:', error);
        document.getElementById('profileInfo').innerHTML = `<p style="color:red;">Failed to load profile.</p>`;
    }
}

function renderStudentStatus(status) {
    switch (status) {
        case 0: return 'Enrolled';
        case 1: return 'On leave';
        case 2: return 'Suspended';
        case 3: return 'Dropped out';
        case 4: return 'Graduated';
        default: return 'Unknown';
    }
}

// Auto-call khi load page
window.onload = loadStudentProfile;
