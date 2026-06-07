package com.educore.bookscodes;

import com.educore.bookscodes.dto.BooksCodesRequest;
import com.educore.common.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books-codes")
@RequiredArgsConstructor
public class BooksCodesController {

    private final BooksCodesRepository repo;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<BooksCodesLocation>>> getAll() {
        return ResponseEntity.ok(GlobalResponse.success(repo.findByActiveTrueOrderByNameAsc()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<BooksCodesLocation>> create(@RequestBody BooksCodesRequest req) {
        BooksCodesLocation loc = BooksCodesLocation.builder()
            .name(req.getName())
            .type(parseType(req.getType()))
            .address(req.getAddress())
            .phone(req.getPhone())
            .sellsBooks(req.isSellsBooks())
            .sellsCodes(req.isSellsCodes())
            .notes(req.getNotes())
            .active(true)
            .build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(GlobalResponse.success("تم الإضافة", repo.save(loc)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<BooksCodesLocation>> update(@PathVariable Long id, @RequestBody BooksCodesRequest req) {
        BooksCodesLocation loc = repo.findById(id).orElseThrow();
        loc.setName(req.getName());
        loc.setType(parseType(req.getType()));
        loc.setAddress(req.getAddress());
        loc.setPhone(req.getPhone());
        loc.setSellsBooks(req.isSellsBooks());
        loc.setSellsCodes(req.isSellsCodes());
        loc.setNotes(req.getNotes());
        return ResponseEntity.ok(GlobalResponse.success("تم التحديث", repo.save(loc)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> delete(@PathVariable Long id) {
        BooksCodesLocation loc = repo.findById(id).orElseThrow();
        loc.setActive(false);
        repo.save(loc);
        return ResponseEntity.ok(GlobalResponse.success("تم الحذف", null));
    }

    private BooksCodesLocation.LocationType parseType(String t) {
        try { return BooksCodesLocation.LocationType.valueOf(t); }
        catch (Exception e) { return BooksCodesLocation.LocationType.CENTER; }
    }
}
