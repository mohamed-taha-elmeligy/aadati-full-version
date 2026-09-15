package com.mts.aadati.services;

import com.mts.aadati.dto.mapper.RoleMapper;
import com.mts.aadati.dto.request.RoleRequest;
import com.mts.aadati.entities.Role;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private static final String ROLE_ALREADY_EXISTS = "Role already exists";
    private static final String ROLE_NOT_FOUND = "Role not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final RoleRepository repository;
    private final RoleMapper mapper;

    @Transactional
    public Role addRole(Role role){
        if (repository.existsByName(role.getName()))
            throw new DuplicateResourceException(ROLE_ALREADY_EXISTS);

        return repository.save(role);
    }

    @Transactional
    public Role updateRole(UUID roleId, RoleRequest request){
        Role role = getRoleOrThrow(roleId);

        if (repository.existsByNameAndRoleIdNot(request.name(),roleId))
            throw new DuplicateResourceException(ROLE_ALREADY_EXISTS);

        mapper.update(request,role);

        return repository.save(role);
    }

    @Transactional
    public Role toggleDeleted(UUID roleId){
        Role role = getRoleOrThrow(roleId);
        role.setDeleted(!role.isDeleted());

        return repository.save(role);
    }

    public List<Role> searchByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Role name " + CAN_NOT_BE_NULL);

        return repository.findByNameContainingIgnoreCase(name);
    }

    public Role findByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Role name " + CAN_NOT_BE_NULL);

        return repository.findByNameIgnoreCase(name)
                .orElseThrow(()->
                        new ResourceNotFoundException(ROLE_NOT_FOUND + "name")
                );
    }

    public List<Role> findAllDeletedFalse() {
        return repository.findAllByIsDeletedFalse();
    }

    public List<Role> findAllDeletedTrue() {
        return repository.findAllByIsDeletedTrue();
    }

    public long countDeletedFalse() {
        return repository.countByIsDeletedFalse();
    }

    private Role getRoleOrThrow(UUID roleId)
    {
        if (roleId == null)
            throw new InvalidRequestException("Role Id " + CAN_NOT_BE_NULL);

        return repository.findById(roleId)
                .orElseThrow(()-> new ResourceNotFoundException(ROLE_NOT_FOUND + "id"));
    }
}
