package model.datasupport;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.Duration;

@Getter
@AllArgsConstructor
public class ExamSchedule {
    private String subjectId;
    private String termId;
    private String gradeName;
    private String examName;
    private LocalDateTime examDate;
    private Duration examDuration;
    private String roomId;
    private int examType;
}
