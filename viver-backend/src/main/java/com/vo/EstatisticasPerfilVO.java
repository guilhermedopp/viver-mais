package com.vo;

public class EstatisticasPerfilVO {
    public int totalPosts;
    public int totalSeguidores;
    public int totalSeguindo;

    public EstatisticasPerfilVO(int totalPosts, int totalSeguidores, int totalSeguindo) {
        this.totalPosts = totalPosts;
        this.totalSeguidores = totalSeguidores;
        this.totalSeguindo = totalSeguindo;
    }
}