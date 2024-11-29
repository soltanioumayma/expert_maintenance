package com.example.test;

public class HistoriqueItem {
    private final String terminee;
    private final String datePlanification;
    private final String commentaire;

    public HistoriqueItem(String terminee, String datePlanification, String commentaire) {
        this.terminee = terminee;
        this.datePlanification = datePlanification;
        this.commentaire = commentaire;
    }

    public String getTerminee() {
        return terminee;
    }

    public String getDatePlanification() {
        return datePlanification;
    }

    public String getCommentaire() {
        return commentaire;
    }
}
