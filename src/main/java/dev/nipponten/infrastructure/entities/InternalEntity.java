package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "internal")
public class InternalEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "internal_role_id")
    private InternalRoleEntity internalRole;

    private String name;

    private String lastName;

    private String cpf;

    protected InternalEntity() {}

    protected InternalEntity(
            UserEntity user, InternalRoleEntity internalRole, String name, String lastName, String cpf) {
        this.user = user;
        this.internalRole = internalRole;
        this.name = name;
        this.lastName = lastName;
        this.cpf = cpf;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public InternalRoleEntity getInternalRole() {
        return internalRole;
    }

    public void setInternalRole(InternalRoleEntity internalRole) {
        this.internalRole = internalRole;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
}
