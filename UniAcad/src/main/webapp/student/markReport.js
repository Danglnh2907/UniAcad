function fetchMarkReport() {
    fetch("/api/student/markReport")
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch");
            return res.json();
        })
        .then(data => renderTable(data))
        .catch(() => {
            document.getElementById('reportTable').style.display = 'none';
            document.getElementById('noData').style.display = 'block';
        });
}

function renderTable(data) {
    const tbody = document.getElementById('reportBody');
    tbody.innerHTML = '';

    data.forEach(item => {
        const row = `<tr>
      <td>${item.studentId}</td>
      <td>${item.studentName}</td>
      <td>${item.subjectName}</td>
      <td>${item.mark}</td>
      <td>${item.status}</td>
    </tr>`;
        tbody.innerHTML += row;
    });

    document.getElementById('noData').style.display = 'none';
    document.getElementById('reportTable').style.display = 'table';
}
