package com.example.test;

public class Intervention {
    private int id;
    private String title;
    private String clientName;
    private String clientAddress;
    private String startTime;
    private String endTime;
    private boolean completed;


    public Intervention(int id, String title, String clientName, String clientAddress, String startTime, String endTime, boolean completed) {
        this.id = id;
        this.title = title;
        this.clientName = clientName;
        this.clientAddress = clientAddress;
        this.startTime = startTime;
        this.endTime = endTime;
        this.completed = completed;
    }

    public int getId() {
        return id;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Intervention(String title, String clientName, String clientAddress, String s, boolean completed) {
        this.title = title;
        this.clientName = clientName;
        this.clientAddress = clientAddress;
        this.startTime = s;
        this.completed = completed;
    }


    public String getTitle() {
        return title;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public boolean isCompleted() {
        return completed;
    }



}
