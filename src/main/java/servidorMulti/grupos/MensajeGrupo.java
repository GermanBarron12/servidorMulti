package servidorMulti.grupos;

import java.sql.Timestamp;

public class MensajeGrupo {
    private int id;
    private int grupoId;
    private String usuario;
    private String mensaje;
    private Timestamp fecha;
    
    public MensajeGrupo(int id, int grupoId, String usuario, String mensaje, Timestamp fecha) {
        this.id = id;
        this.grupoId = grupoId;
        this.usuario = usuario;
        this.mensaje = mensaje;
        this.fecha = fecha;
    }
    
    public int getId() {
        return id;
    }
    
    public int getGrupoId() {
        return grupoId;
    }
    
    public String getUsuario() {
        return usuario;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public Timestamp getFecha() {
        return fecha;
    }
    
    public String formatear() {
        return "[" + usuario + "]: " + mensaje;
    }
}