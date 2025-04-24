package model;

public class Term {
    private String termId;
    private String termName;

    public Term(String termId, String termName) {
        this.termId = termId;
        this.termName = termName;
    }

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
