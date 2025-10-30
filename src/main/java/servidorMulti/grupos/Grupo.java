package servidorMulti.grupos;

public class Grupo {
    private int id;
    private String nombre;
    private String creador;
    private boolean esSistema;
    
    public Grupo(int id, String nombre, String creador, boolean esSistema) {
        this.id = id;
        this.nombre = nombre;
        this.creador = creador;
        this.esSistema = esSistema;
    }
    
    public int getId() {
        return id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public String getCreador() {
        return creador;
    }
    
    public boolean isEsSistema() {
        return esSistema;
    }
    
    @Override
    public String toString() {
        return nombre + (esSistema ? " [Sistema]" : "");
    }
}   