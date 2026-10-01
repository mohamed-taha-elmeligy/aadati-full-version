package com.mts.aadati.controllers.admins;

import com.mts.aadati.dto.mapper.RoleMapper;
import com.mts.aadati.dto.request.RoleRequest;
import com.mts.aadati.dto.response.RoleResponse;
import com.mts.aadati.services.RoleService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.ADMIN + Uri.ROLE)
@RequiredArgsConstructor
@Validated
public class RoleController {

    private final RoleService roleService;
    private final RoleMapper roleMapper;

    @PostMapping("/create")
    public ResponseEntity<RoleResponse> create(
            @Valid @RequestBody RoleRequest roleRequest
    ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        roleMapper.toResponse(
                                roleService.addRole(roleMapper.toEntity(roleRequest))
                        )
                );
    }

    @PutMapping("/update/id/{id}")
    public ResponseEntity<RoleResponse> update(
            @PathVariable("id") UUID roleId,
            @Valid @RequestBody RoleRequest roleRequest
    ){
        return ResponseEntity.ok(
                roleMapper.toResponse(
                        roleService.updateRole(roleId,roleRequest)
                )
        );
    }

    @PutMapping("/toggle/delete/id/{id}")
    public ResponseEntity<RoleResponse> toggleDeleted(
            @PathVariable("id") UUID roleId
    ){
        return ResponseEntity.ok(
                roleMapper.toResponse(
                        roleService.toggleDeleted(roleId)
                )
        );
    }

    @GetMapping("/search/name/{name}")
    public ResponseEntity<List<RoleResponse>> searchByName(
            @NotBlank @PathVariable("name") String name
    ){
        return ResponseEntity.ok(
                roleMapper.toResponseList(
                        roleService.searchByName(name)
                )
        );
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RoleResponse> getByName(
            @NotBlank @PathVariable("name") String name
    ){
        return ResponseEntity.ok(
                roleMapper.toResponse(
                        roleService.findByName(name)
                )
        );
    }

    @GetMapping("/all")
    public ResponseEntity<List<RoleResponse>> getAllDeletedFalse(){
        return ResponseEntity.ok(
                roleMapper.toResponseList(
                        roleService.findAllDeletedFalse()
                )
        );
    }

    @GetMapping("/all/deleted")
    public ResponseEntity<List<RoleResponse>> getAllDeletedTrue(){
        return ResponseEntity.ok(
                roleMapper.toResponseList(
                        roleService.findAllDeletedTrue()
                )
        );
    }

    @GetMapping("/count/undeleted")
    public ResponseEntity<Long> countDeletedFalse(){
        return ResponseEntity.ok(
                        roleService.countDeletedFalse()
        );
    }

}
