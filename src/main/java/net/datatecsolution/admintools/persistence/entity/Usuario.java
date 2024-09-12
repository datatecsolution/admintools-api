package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String idUsuario;

    @Column(name = "usuario")
    private String usuario;


}
