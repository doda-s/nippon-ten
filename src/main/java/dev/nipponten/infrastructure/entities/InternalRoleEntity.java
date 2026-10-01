package dev.nipponten.infrastructure.entities;

import java.util.List;

import dev.nipponten.domain.models.InternalPermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "internal_role")
public class InternalRoleEntity {

    @Id @GeneratedValue private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "permissions")
    private List<InternalPermission> permissions;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean defaultRole = false;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    public InternalRoleEntity() {}

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InternalPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<InternalPermission> permissions) {
        this.permissions = permissions;
    }

    public boolean isDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(boolean defaultRole) {
        this.defaultRole = defaultRole;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
