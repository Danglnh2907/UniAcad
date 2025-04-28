function getMonday(date) {
    const day = date.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    const monday = new Date(date);
    monday.setDate(date.getDate() + diff);
    return monday;
}

function formatDate(date) {
    return date.toISOString().split('T')[0];
}

async function loadWeeklySchedule(startDate) {
    try {
        const monday = getMonday(startDate);
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 7);

        const response = await fetch(window.location.origin + `/UniAcad/api/student/timetable?start=${formatDate(monday)}&end=${formatDate(sunday)}`);
        const schedules = await response.json();

        renderTimetable(schedules, monday);
    } catch (error) {
        console.error('Error loading schedule:', error);
        document.getElementById('scheduleBody').innerHTML = "<tr><td colspan='8'>Failed to load schedule.</td></tr>";
    }
}

function renderTimetable(schedules, monday) {
    const tbody = document.getElementById('scheduleBody');
    tbody.innerHTML = "";

    const slots = 8;
    const daysOfWeek = 7;

    const tableData = Array.from({ length: slots }, () => Array(daysOfWeek).fill(null));

    schedules.forEach(item => {
        const startTime = new Date(item.startTime);
        const slotNumber = getSlotNumber(startTime.getHours(), startTime.getMinutes());
        const dayIndex = (startTime.getDay() + 6) % 7;

        if (slotNumber !== -1 && dayIndex >= 0 && dayIndex <= 6) {
            let attendStatus = "not-marked";
            let attendText = "Not marked";

            if (item.hasOwnProperty('attendStatus') && item.attendStatus !== null) {
                attendStatus = item.attendStatus ? "present" : "absent";
                attendText = item.attendStatus ? "Present" : "Absent";
            }

            tableData[slotNumber][dayIndex] = {
                subjectName: item.subjectName,
                roomId: item.roomId,
                attendStatus: attendStatus,
                attendText: attendText
            };
        }
    });

    const today = new Date();
    const todayIndex = (today.getDay() + 6) % 7;

    for (let slot = 0; slot < slots; slot++) {
        const tr = document.createElement('tr');

        const slotTd = document.createElement('td');
        slotTd.innerHTML = `Slot ${slot + 1}`;
        tr.appendChild(slotTd);

        for (let day = 0; day < daysOfWeek; day++) {
            const td = document.createElement('td');

            if (tableData[slot][day]) {
                td.className = tableData[slot][day].attendStatus;
                td.innerHTML = `
                    <div class="tooltip">
                        ${tableData[slot][day].subjectName}<br>${tableData[slot][day].roomId}<br><small>(${tableData[slot][day].attendText})</small>
                        <div class="tooltip-text">
                            ${tableData[slot][day].subjectName}<br>
                            Room: ${tableData[slot][day].roomId}<br>
                            Status: ${tableData[slot][day].attendText}
                        </div>
                    </div>
                `;
            } else {
                td.innerHTML = "-";
            }

            if (day === todayIndex) {
                td.classList.add("today-cell");
            }

            tr.appendChild(td);
        }
        tbody.appendChild(tr);
    }
}

function getSlotNumber(hour, minute) {
    const timeInMinutes = hour * 60 + minute;
    if (timeInMinutes >= 7 * 60 && timeInMinutes < 8 * 60 + 30) return 0;
    if (timeInMinutes >= 8 * 60 + 30 && timeInMinutes < 10 * 60) return 1;
    if (timeInMinutes >= 10 * 60 && timeInMinutes < 11 * 60 + 30) return 2;
    if (timeInMinutes >= 11 * 60 + 30 && timeInMinutes < 13 * 60) return 3;
    if (timeInMinutes >= 13 * 60 && timeInMinutes < 14 * 60 + 30) return 4;
    if (timeInMinutes >= 14 * 60 + 30 && timeInMinutes < 16 * 60) return 5;
    if (timeInMinutes >= 16 * 60 && timeInMinutes < 17 * 60 + 30) return 6;
    if (timeInMinutes >= 17 * 60 + 30 && timeInMinutes < 19 * 60) return 7;
    return -1;
}

function reloadSchedule() {
    const input = document.getElementById('startDate');
    const selectedDate = new Date(input.value);
    loadWeeklySchedule(selectedDate);
}

window.onload = function() {
    const today = new Date();
    document.getElementById('startDate').value = formatDate(today);
    loadWeeklySchedule(today);
};
