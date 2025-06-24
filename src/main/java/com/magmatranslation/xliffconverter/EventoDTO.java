package com.magmatranslation.xliffconverter;

public class EventoDTO {
    private String nome;
    private String data;

    // Construtor padrão (obrigatório para deserialização)
    public EventoDTO() {}

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
