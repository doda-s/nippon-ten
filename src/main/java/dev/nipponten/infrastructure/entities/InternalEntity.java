package dev.nipponten.infrastructure.entities;

import java.util.List;

import dev.nipponten.domain.models.InternalPermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "internal")
public class InternalEntity {

    @Id @GeneratedValue private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "internal_role_id")
    private InternalRoleEntity internalRole;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "user_permissions")
    private List<InternalPermission> userPermissions;
    
    private String name;
    
    private String lastName;
    
    private String cpf;

    public InternalEntity() {}
    
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

    public void setInternalRole(InternalRoleEntity internalPermission) {
        this.internalRole = internalPermission;
    }
    
    public List<InternalPermission> getUserPermissions() {
        return userPermissions;
    }

    public void setUserPermissions(List<InternalPermission> userPermissions) {
        this.userPermissions = userPermissions;
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
