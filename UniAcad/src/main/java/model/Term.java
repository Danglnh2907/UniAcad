package model;

public class Term {
    private String termId; // CHAR(4)
    private String termName; // VARCHAR

    // Constructor
    public Term() {
    }
    public Term(String termId, String termName) {
        this.termId = termId;
        this.termName = termName;
    }

    // Getters and Setters
    public String getTermId() {
        return termId;
    }

    public void setTermId(String termId) {
        this.termId = termId;
    }

    public String getTermName() {
        return termName;
    }

    public void setTermName(String termName) {
        this.termName = termName;
    }
}