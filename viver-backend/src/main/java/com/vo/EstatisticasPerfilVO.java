package com.vo;

// Encapsulamento: campos PRIVATE com getters públicos
public class EstatisticasPerfilVO {
    private int totalPosts;
    private int totalSeguidores;
    private int totalSeguindo;

    public EstatisticasPerfilVO(int totalPosts, int totalSeguidores, int totalSeguindo) {
        this.totalPosts = totalPosts;
        this.totalSeguidores = totalSeguidores;
        this.totalSeguindo = totalSeguindo;
    }

    public int getTotalPosts() { return totalPosts; }
    public int getTotalSeguidores() { return totalSeguidores; }
    public int getTotalSeguindo() { return totalSeguindo; }
}