USE [master]
GO

/*******************************************************************************
   Drop database if it exists
********************************************************************************/
IF EXISTS (SELECT name FROM master.dbo.sysdatabases WHERE name = N'UniAcad')
BEGIN
	ALTER DATABASE [UniAcad] SET OFFLINE WITH ROLLBACK IMMEDIATE;
	ALTER DATABASE [UniAcad] SET ONLINE;
	DROP DATABASE [UniAcad];
END

GO

CREATE DATABASE [UniAcad]
GO

USE [UniAcad]
GO

/*******************************************************************************
	Drop tables if exists
*******************************************************************************/
DECLARE @sql nvarchar(MAX) 
SET @sql = N'' 

SELECT @sql = @sql + N'ALTER TABLE ' + QUOTENAME(KCU1.TABLE_SCHEMA) 
    + N'.' + QUOTENAME(KCU1.TABLE_NAME) 
    + N' DROP CONSTRAINT ' -- + QUOTENAME(rc.CONSTRAINT_SCHEMA)  + N'.'  -- not in MS-SQL
    + QUOTENAME(rc.CONSTRAINT_NAME) + N'; ' + CHAR(13) + CHAR(10) 
FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS AS RC 

INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS KCU1 
    ON KCU1.CONSTRAINT_CATALOG = RC.CONSTRAINT_CATALOG  
    AND KCU1.CONSTRAINT_SCHEMA = RC.CONSTRAINT_SCHEMA 
    AND KCU1.CONSTRAINT_NAME = RC.CONSTRAINT_NAME 

EXECUTE(@sql) 

GO
DECLARE @sql2 NVARCHAR(max)=''

SELECT @sql2 += ' Drop table ' + QUOTENAME(TABLE_SCHEMA) + '.'+ QUOTENAME(TABLE_NAME) + '; '
FROM   INFORMATION_SCHEMA.TABLES
WHERE  TABLE_TYPE = 'BASE TABLE'

Exec Sp_executesql @sql2 
GO

/*******************************************************************************
   Create table
********************************************************************************/
CREATE TABLE Major
(
  MajorID CHAR(2) NOT NULL,
  MajorName VARCHAR(100) NOT NULL,
  PRIMARY KEY (MajorID)
);

CREATE TABLE Term
(
  TermID CHAR(4) NOT NULL,
  TermName VARCHAR(50) NOT NULL,
  PRIMARY KEY (TermID)
);

CREATE TABLE Department
(
  DepartmentID CHAR(2) NOT NULL,
  DepartmentName VARCHAR(100) NOT NULL,
  PRIMARY KEY (DepartmentID)
);

CREATE TABLE Curriculum
(
  CurriculumID VARCHAR(50) NOT NULL,
  CurriculumName VARCHAR(255) NOT NULL,
  MajorID CHAR(2) NOT NULL,
  PRIMARY KEY (CurriculumID),
  FOREIGN KEY (MajorID) REFERENCES Major(MajorID)
);

CREATE TABLE Teacher
(
  TeacherID VARCHAR(20) NOT NULL,
  TeacherEmail VARCHAR(50) NOT NULL,
  TeacherName VARCHAR(100) NOT NULL,
  TeacherPhone VARCHAR(20) NOT NULL,
  TeacherStatus INT DEFAULT 0, --0 Active, 1 On leave, 2 Suspended, 3 Resigned
  DepartmentID CHAR(2),
  PRIMARY KEY (TeacherID),
  FOREIGN KEY (DepartmentID) REFERENCES Department(DepartmentID),
  UNIQUE (TeacherEmail)
);

CREATE TABLE Room
(
  RoomID CHAR(4) NOT NULL,
  PRIMARY KEY (RoomID)
);

CREATE TABLE Staff
(
  StaffID VARCHAR(20) NOT NULL,
  StaffName VARCHAR(100) NOT NULL,
  StaffEmail VARCHAR(50) NOT NULL,
  StaffPhone VARCHAR(20) NOT NULL,
  StaffStatus INT DEFAULT 0, --0 Active, 1 On leave, 2 Suspended, 3 Resigned
  PRIMARY KEY (StaffID)
);

CREATE TABLE Student
(
  StudentID CHAR(8) NOT NULL,
  StudentEmail VARCHAR(50) NOT NULL,
  StudentName VARCHAR(100) NOT NULL,
  StudentDoB DATE NOT NULL,
  StudentGender BIT NOT NULL, --0 Male, 1 Female--
  StudentSSN VARCHAR(20) NOT NULL,
  [Address] VARCHAR(255),
  StudentStatus INT DEFAULT 0, -- 0 Enrolled, 1 On leave, 2 Suspended , 3 Dropped out, 4 Graduated
  StudentPhone VARCHAR(20) NOT NULL,
  CurriculumID VARCHAR(50) NOT NULL,
  PRIMARY KEY (StudentID),
  FOREIGN KEY (CurriculumID) REFERENCES Curriculum(CurriculumID),
  UNIQUE (StudentEmail, StudentSSN)
);

CREATE TABLE [Subject]
(
  SubjectID CHAR(7) NOT NULL,
  SubjectName VARCHAR(100) NOT NULL,
  Credits INT,
  DepartmentID CHAR(2),
  IsDeleted BIT DEFAULT 0,
  PRIMARY KEY (SubjectID),
  FOREIGN KEY (DepartmentID) REFERENCES Department(DepartmentID)
);

CREATE TABLE Course
(
  CourseID INT IDENTITY NOT NULL,
  ClassID CHAR(6),
  SubjectID CHAR(7) NOT NULL,
  TermID CHAR(4) NOT NULL,
  TotalSlot INT NOT NULL,
  CourseStatus BIT NOT NULL DEFAULT 0, --0 Available, 1 Overdue
  PRIMARY KEY (CourseID),
  FOREIGN KEY (SubjectID) REFERENCES Subject(SubjectID),
  FOREIGN KEY (TermID) REFERENCES Term(TermID)
);

CREATE TABLE Slot
(
  SlotNumber INT NOT NULL,
  StartTime DATETIME,
  Duration TIME,
  CourseID INT,
  TeacherID VARCHAR(20),
  RoomID CHAR(4),
  PRIMARY KEY (SlotNumber, CourseID),
  FOREIGN KEY (CourseID) REFERENCES Course(CourseID),
  FOREIGN KEY (TeacherID) REFERENCES Teacher(TeacherID),
  FOREIGN KEY (RoomID) REFERENCES Room(RoomID)
);

CREATE TABLE Grade
(
  GradeID INT IDENTITY,
  GradeName VARCHAR(255),
  CourseID INT NOT NULL,
  GradePercent INT NOT NULL,
  PRIMARY KEY (GradeID),
  FOREIGN KEY (CourseID) REFERENCES Course(CourseID),
  CHECK (GradePercent > 0 AND GradePercent <= 100)
);

CREATE TABLE Mark
(
	GradeID INT NOT NULL,
	StudentID CHAR(8) NOT NULL,
	Mark DECIMAL(3,1),
	PRIMARY KEY (GradeID, StudentID),
	FOREIGN KEY (GradeID) REFERENCES Grade(GradeID),
	FOREIGN KEY (StudentID) REFERENCES Student(StudentID),
	CHECK (Mark >= 0 AND Mark <= 10)
)

CREATE TABLE [Include]
(
  Semester INT NOT NULL,
  CurriculumID VARCHAR(50) NOT NULL,
  SubjectID CHAR(7) NOT NULL,
  PRIMARY KEY (CurriculumID, SubjectID),
  FOREIGN KEY (CurriculumID) REFERENCES Curriculum(CurriculumID),
  FOREIGN KEY (SubjectID) REFERENCES Subject(SubjectID)
);

CREATE TABLE Study
(
  StudentID CHAR(8) NOT NULL,
  CourseID INT NOT NULL,
  PRIMARY KEY (StudentID, CourseID),
  FOREIGN KEY (StudentID) REFERENCES Student(StudentID),
  FOREIGN KEY (CourseID) REFERENCES Course(CourseID)
);

CREATE TABLE Attendent
(
  Status BIT DEFAULT NULL, --NULL Not yet, 0 Absent, 1 Attendent
  StudentID CHAR(8) NOT NULL,
  SlotNumber INT NOT NULL,
  CourseID INT NOT NULL,
  PRIMARY KEY (StudentID, CourseID, SlotNumber),
  FOREIGN KEY (StudentID) REFERENCES Student(StudentID),
  FOREIGN KEY (SlotNumber, CourseID) REFERENCES Slot(SlotNumber, CourseID)
);

CREATE TABLE Fee
(
	FeeID INT IDENTITY,
	StudentID CHAR(8) NOT NULL,
	TermID CHAR(4),
	Amount MONEY NOT NULL,
	DueDate DATETIME,
	FeeStatus INT NOT NULL,
	PRIMARY KEY (FeeID),
	FOREIGN KEY (StudentID) REFERENCES Student(StudentID),
	FOREIGN KEY (TermID) REFERENCES Term(TermID)
);

CREATE TABLE Payment
(
	PaymentID INT IDENTITY,
	FeeID INT NOT NULL,
	PaymentDate DATETIME,
	AmountPaid MONEY NOT NULL,
	PaymentStatus INT NOT NULL,
	PRIMARY KEY (PaymentID),
	FOREIGN KEY (FeeID) REFERENCES Fee(FeeID)
);

CREATE TABLE GradeReport
(
	SubjectID CHAR(7) NOT NULL,
	StudentID CHAR(8) NOT NULL,
	TermID CHAR(4),
	Mark DECIMAL(3,1),
	GradeReportStatus INT,
	PRIMARY KEY (SubjectID, StudentID),
	FOREIGN KEY (SubjectID) REFERENCES [Subject](SubjectID),
	FOREIGN KEY (StudentID) REFERENCES Student(StudentID),
	FOREIGN KEY (TermID) REFERENCES Term(TermID),
	CHECK (Mark >= 0 AND Mark <= 10)
)

CREATE TABLE Exam
(
	ExamID INT IDENTITY,
	ExamName VARCHAR(255),
	GradeID INT NOT NULL,
	ExamDate DATETIME NOT NULL,
	ExamDuration TIME NOT NULL,
	RoomID CHAR(4),
	ExamType INT, -- 0 Multiple Choices, 1 Practical Exam, 2 Speaking, 3 Presentation
	PRIMARY KEY (ExamID),
	FOREIGN KEY (GradeID) REFERENCES Grade(GradeID),
	FOREIGN KEY (RoomID) REFERENCES Room(RoomID)
)

GO

/*******************************************************************************
   TRIGGER
********************************************************************************/





/*******************************************************************************
   Example Data
********************************************************************************/
INSERT INTO Major(MajorID, MajorName) VALUES
('SE', 'Software Engineering'),
('GD', 'Graphic Design'),
('IB', 'International Business')

INSERT INTO Curriculum(CurriculumID, CurriculumName, MajorID) VALUES
('BIT_SE_K18D_19A', 'The Bachelor Program of Information Technology, Software Engineering Major', 'SE'),
('BIT_GD_K19DK20A','Bachelor Program of Information Technology, Digital Art & Design Major','GD'),
('BBA_IB_K18C','Bachelor Program of Business Adminstration, International Business Major','IB')

INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('AI', 'Artificial Intelligence');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('EL', 'English');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('FN', 'Finance');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('GR', 'Graduate');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('GD', 'Graphic Design');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('IA', 'Information Assurance');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('IB', 'International Business');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('JP', 'Japanese');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('MG', 'Management');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('MA', 'Mathematics');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('JT', 'On the job training');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('PT', 'Physical Training');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('SS', 'Soft Skill');
INSERT INTO Department(DepartmentID, DepartmentName) VALUES ('SE', 'Software Engineering');

INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ENT001', 'English 1 (Topnotch Fundamental)', 'EL', '0');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ENT104', 'English 2 (Top Notch 1)', 'EL', '0');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ENT203', 'English 3 (Top Notch 2)', 'EL', '0');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ENT303', 'English 4 (Top Notch 3)', 'EL', '0');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ENT403', 'English 5 (Summit 1)', 'EL', '0');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ENT503', 'English 6 (Summit 2)', 'EL', '0');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('VOV114', 'Vovinam 1', 'PT', '2');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('VOV124', 'Vovinam 2', 'PT', '2');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('VOV134', 'Vovinam 3', 'PT', '2');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('MAE101', 'Mathematics for Engineering', 'MA', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PRF192', 'Programming Fundamentals', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('CEA201', 'Computer Organization and Architecture', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SSL101c', 'Academic skills for University success', 'SS', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('CSI104', 'Introduction to Computer Science', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SSG104', 'Communication and In-Group Working Skills', 'SS', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('MAD101', 'Discrete mathematics', 'MA', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PRO192', 'Object-Oriented Programming', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('OSG202', 'Operating System', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('NWC204', 'Computer Networking', 'IA', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('CSD201', 'Data Structures and Algorithm', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('LAB211', 'OOP with Java Lab', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('DBI202', 'Database Systems', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('WED201c', 'Web design', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('JPD113', 'Elementary Japanese 1- A1.1', 'JP', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('IOT102', 'Internet of Things', 'AI', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PRJ301', 'Java Web Application Development', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SWE201c', 'Introduction to Software Engineering', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('JPD123', 'Elementary Japanese 1-A1.2', 'JP', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('MAS291', 'Statistics & Probability', 'MA', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SWR302', 'Software Requirement', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SWT301', 'Software Testing', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SWP391', 'Software development project', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('WDU203c', 'UI/UX Design', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PRN212', 'Basic Cross-Platform Application Programming With .NET', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('OJT202', 'On-The-Job Training', 'JT', '0');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PMG201c', 'Project management', 'MG', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SWD392', 'Software Architecture and Design', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PRU212', 'C# Programming and Unity', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PRN222', 'Advanced Cross-Platform Application Programming With .NET', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PRN232', 'Building Cross-Platform Back-End Application With .NET', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PRM392', 'Mobile Programming', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ITE302c', 'Ethics in IT', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SE_GRA', 'Graduation - Software Engineering', 'SE', '10');

INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('DRP101', 'Drawing plaster stature - portrait', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('DRS102', 'Drawing - Form, Still-life', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('DTG102', 'Visual Design Tools', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('VCM202', 'Visual Communication', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('AFA201', 'Human Anatomy for Artis', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('GDF201', 'Fundamental of Graphic Design', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('HOA102', 'Art History', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PST202', 'Perspective', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ANS201', 'Idea & Script Development', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('PFD201', 'Photography for Designer', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('RMD301', 'Reseach Methods For Designers', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('TPG203', 'Basic typography & Layout', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ANC302', 'Character Design', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('DTG303', 'Principles of Animation', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('TPG302', 'Typography & E-publication Design', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('WDU202c', 'UI/UX Design', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('CAA201', 'Communications and advertising', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('DTG304', 'Principles of Compositing', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SDP201', 'Sound Production', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('AET102c', 'Aesthetic', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('EXE201', 'Experiential Entrepreneurship 2', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('HOD102', 'Design History', 'GD', '2');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('IPR102', 'Intellectual Property Rights', 'GD', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('GD_GRA', 'Graduation Elective for GD', 'GD', '10');

INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ECO111', 'Microeconomics', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ENM302', 'Business English - Level 1', 'EL', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('MGT103', 'Introduction to Management', 'MG', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('MKT101', 'Marketing Principles', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ACC101', 'Principles of Accounting', 'FN', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ECO121', 'Macroeconomics', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ENM402', 'Business English - Level 2', 'EL', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('OBE102c', 'Organizational Behavior', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ECO201', 'International Economics', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('FIN202', 'Principles of Corporate Finance', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('HRM202c', 'Human Resource Management', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('IBC201', 'Cross Cultural Management and Negotiation', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('IBI101', 'Introduction to International Business', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('IBF301', 'International Finance', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('ITA203c', 'Information System Overview', 'SE', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('MAS202', 'Applied Statistics for Business', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SCM201', 'Supply Chain Management', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('IBS301m', 'International Business Strategy', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('IEI301', 'Import Export', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('MKT205c', 'International Marketing', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('SSB201', 'Advanced Business Communication', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('LAW102', 'Business Law and Ethics Fundamentals', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('RMB301', 'Business Research Methods', 'IB', '3');
INSERT INTO Subject(SubjectID, SubjectName, DepartmentID, Credits) VALUES ('IB_GRA', 'Graduation Elective - International Business', 'IB', '10');

INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'ENT001', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'ENT104', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'ENT203', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'ENT303', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'ENT403', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'ENT503', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'VOV114', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'VOV124', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'VOV134', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'MAE101', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PRF192', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'CEA201', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'SSL101c', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'CSI104', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'SSG104', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'MAD101', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PRO192', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'OSG202', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'NWC204', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'CSD201', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'LAB211', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'DBI202', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'WED201c', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'JPD113', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'IOT102', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PRJ301', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'SWE201c', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'JPD123', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'MAS291', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'SWR302', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'SWT301', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'SWP391', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'WDU203c', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PRN212', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'OJT202', '6');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PMG201c', '7');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'SWD392', '7');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PRU212', '7');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PRN222', '7');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PRN232', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'PRM392', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'ITE302c', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_SE_K18D_19A', 'SE_GRA', '9');

INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'ENT001', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'ENT104', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'ENT203', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'ENT303', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'ENT403', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'ENT503', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'VOV114', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'DRP101', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'DRS102', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'DTG102', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'VOV124', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'SSL101c', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'VCM202', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'AFA201', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'GDF201', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'HOA102', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'VOV134', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'PST202', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'ANS201', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'JPD113', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'PFD201', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'RMD301', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'TPG203', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'ANC302', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'DTG303', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'JPD123', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'TPG302', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'WDU202c', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'CAA201', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'DTG304', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'SSG104', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'OJT202', '6');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'SDP201', '7');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'AET102c', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'EXE201', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'HOD102', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'IPR102', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BIT_GD_K19DK20A', 'GD_GRA', '9');

INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ENT001', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ENT104', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ENT203', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ENT303', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ENT403', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ENT503', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'VOV114', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ECO111', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ENM302', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'MGT103', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'MKT101', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'VOV124', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'SSL101c', '1');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ACC101', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ECO121', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ENM402', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'OBE102c', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'VOV134', '0');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'SSG104', '2');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ECO201', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'FIN202', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'HRM202c', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'IBC201', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'IBI101', '3');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'IBF301', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'ITA203c', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'MAS202', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'SCM201', '4');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'IBS301m', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'IEI301', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'MKT205c', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'SSB201', '5');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'LAW102', '7');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'EXE201', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'PMG201c', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'RMB301', '8');
INSERT INTO Include(CurriculumID, SubjectID, Semester) VALUES ('BBA_IB_K18C', 'IB_GRA', '9');

INSERT INTO Term(TermID, TermName) VALUES
('SP25', 'Spring 25'),
('SU25', 'Summer 25')

INSERT INTO Room(RoomID) VALUES
('G201'), ('G202'), ('G203'), ('G204'), ('G205'), ('G206'), ('G207'), ('G208'), ('G209'), ('G210'), ('G211'), ('G212'), ('G213'),
('G214'), ('G215'), ('G216'), ('G217'), ('G218'), ('G219'), ('G220'), ('G301'), ('G302'), ('G303'), ('G304'), ('G305'), ('G306'),
('G307'), ('G308'), ('G309'), ('G310'), ('G311'), ('G312'), ('G313'), ('G314'), ('G315'), ('G316'), ('G317'), ('G318'), ('G319'),
('G320'), ('G401'), ('G402'), ('G403'), ('G404'), ('G405'), ('G406'), ('G407'), ('G408'), ('G409'), ('G410'), ('G411'), ('G412'),
('G413'), ('G414'), ('G415'), ('G416'), ('G417'), ('G418'), ('G419'), ('G420');

INSERT INTO Teacher(TeacherID, TeacherName, TeacherEmail, TeacherPhone, TeacherStatus, DepartmentID) VALUES
('PhucPT', 'Pham Tien Phuc', 'phucpt@gmail.com', '0123456789', 0, 'SE'),
('KhanhVH', 'Vo Hong Kanh', 'khanhvh@gmail.com', '0234567890', 0, 'SE')

INSERT INTO Student(StudentID, StudentName, StudentSSN, StudentEmail, StudentPhone,  CurriculumID, StudentGender, Address, StudentDoB) VALUES
('CE181480', 'Ho Quang Thanh', '001122334455', 'thanhhqce181480@gmail.com', '07878', 'BIT_SE_K18D_19A', 0, 'CanTho', '2004-09-25'),
('CE182286', 'Nguyen Hoang Khai', '001122337755', 'khainhce182286@fpt.edu.vn', '0787844', 'BIT_SE_K18D_19A', 0, 'CanTho2', '2004-09-26'),
('CE180008', 'Nguyen Thi Anh', '001122334422', 'anhntce180008@gmail.com', '0787855', 'BIT_SE_K18D_19A', 1, 'CanTho3', '2004-09-27'),
('CE180009', 'Nguyen Minh Nhu', '001122337733', 'nhunmce180009@gmail.com', '0787866', 'BIT_SE_K18D_19A', 1, 'CanTho4', '2004-09-28')

INSERT INTO Staff(StaffID, StaffName, StaffEmail, StaffPhone, StaffStatus) VALUES
('S1', 'Staff1', 'Staff1@gmail.com' , '0123123123', 0),
('S2', 'Staff2', 'Staff2@gmail.com' , '0456456456', 0)

INSERT INTO Course(ClassID, SubjectID, TermID, TotalSlot) VALUES
('SE1815', 'SWP391', 'SP25', 10),
('SE1815', 'SWP391', 'SP25', 10)

INSERT INTO Study(CourseID, StudentID) VALUES
(1, 'CE181480'),
(2, 'CE182286'),
(1, 'CE180008'),
(2, 'CE180009')

INSERT INTO Slot(CourseID, SlotNumber, StartTime, Duration, RoomID, TeacherID) VALUES
(1, 1, '2025-05-03 07:00:00', '2:15:00', 'G207', 'PhucPT'),
(1, 2, '2025-05-04 07:00:00', '2:15:00', 'G208', 'PhucPT'),
(1, 3, '2025-05-05 07:00:00', '2:15:00', 'G207', 'PhucPT'),
(1, 4, '2025-05-06 07:00:00', '2:15:00', 'G208', 'PhucPT'),
(1, 5, '2025-05-07 07:00:00', '2:15:00', 'G207', 'PhucPT'),
(1, 6, '2025-05-08 07:00:00', '2:15:00', 'G208', 'PhucPT'),
(1, 7, '2025-05-09 07:00:00', '2:15:00', 'G207', 'PhucPT'),
(1, 8, '2025-05-10 07:00:00', '2:15:00', 'G208', 'PhucPT'),
(1, 9, '2025-05-11 07:00:00', '2:15:00', 'G207', 'PhucPT'),
(1, 10, '2025-05-12 07:00:00', '2:15:00', 'G208', 'PhucPT')

INSERT INTO Slot(CourseID, SlotNumber, StartTime, Duration, RoomID, TeacherID) VALUES
(2, 1, '2025-05-03 07:00:00', '2:15:00', 'G205', 'KhanhVH'),
(2, 2, '2025-05-04 07:00:00', '2:15:00', 'G206', 'KhanhVH'),
(2, 3, '2025-05-05 07:00:00', '2:15:00', 'G205', 'KhanhVH'),
(2, 4, '2025-05-06 07:00:00', '2:15:00', 'G206', 'KhanhVH'),
(2, 5, '2025-05-07 07:00:00', '2:15:00', 'G205', 'KhanhVH'),
(2, 6, '2025-05-08 07:00:00', '2:15:00', 'G206', 'KhanhVH'),
(2, 7, '2025-05-09 07:00:00', '2:15:00', 'G205', 'KhanhVH'),
(2, 8, '2025-05-10 07:00:00', '2:15:00', 'G206', 'KhanhVH'),
(2, 9, '2025-05-11 07:00:00', '2:15:00', 'G205', 'KhanhVH'),
(2, 10, '2025-05-12 07:00:00', '2:15:00', 'G206', 'KhanhVH')

SELECT * FROM Student
SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                COUNT(s.SlotNumber) AS TotalSlots,
                SUM(CASE WHEN a.Status = 0 THEN 1 ELSE 0 END) AS AbsentSlots
            FROM Student st
            JOIN Study st2 ON st.StudentID = st2.StudentID
            JOIN Course c ON st2.CourseID = c.CourseID
            JOIN [Subject] su ON c.SubjectID = su.SubjectID
            JOIN Slot s ON c.CourseID = s.CourseID
            LEFT JOIN Attendent a ON a.StudentID = st.StudentID 
                                   AND a.CourseID = c.CourseID 
                                   AND a.SlotNumber = s.SlotNumber
            WHERE st.StudentEmail = 'khainhce182286@fpt.edu.vn'
            GROUP BY st.StudentID, st.StudentName, su.SubjectName

SELECT * FROM Major