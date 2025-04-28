// studentAttendance.js

async function loadAttendanceReport() {
    try {
        const response = await fetch(window.location.origin + '/UniAcad/api/student/attendance');
        const report = await response.json();
        const tbody = document.getElementById('attendanceBody');
        tbody.innerHTML = '';

        report.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="p-4">${item.subjectName}</td>
                <td class="p-4 text-center">${item.totalSlots}</td>
                <td class="p-4 text-center text-green-600 font-semibold">${item.attendedSlots}</td>
                <td class="p-4 text-center text-red-600 font-semibold">${item.absentSlots}</td>
                <td class="p-4 text-center text-yellow-600 font-semibold">${item.notMarkedSlots}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (error) {
        console.error('Failed to load attendance report:', error);
    }
}

window.onload = loadAttendanceReport;
