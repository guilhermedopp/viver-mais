package com.vo;

// Classe Abstrata (Superclasse)
public abstract class Reacao {
    public abstract String getNome();
    public abstract String getEmoji();
    
    // Design Pattern: Factory Method
    public static Reacao criar(String tipo) {
        if (tipo == null) return new Curtida();
        switch (tipo.toLowerCase()) {
            case "abraco": return new Abraco();
            case "parabens": return new Parabens();
            default: return new Curtida();
        }
    }
}

// Subclasses (Polimorfismo)
class Curtida extends Reacao {
    @Override public String getNome() { return "Curtida"; }
    @Override public String getEmoji() { return "❤️"; }
}

class Abraco extends Reacao {
    @Override public String getNome() { return "Abraço"; }
    @Override public String getEmoji() { return "🫂"; }
}

class Parabens extends Reacao {
    @Override public String getNome() { return "Parabéns"; }
    @Override public String getEmoji() { return "👏"; }
}