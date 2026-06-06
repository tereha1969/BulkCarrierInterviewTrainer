package com.bulkcarrier.interviewtrainer;

public class Question {
    public int id;
    public String section;
    public String q;
    public String a;
    public String ru;

    public Question(int id, String section, String q, String a, String ru) {
        this.id = id;
        this.section = section;
        this.q = q;
        this.a = a;
        this.ru = ru;
    }
}
