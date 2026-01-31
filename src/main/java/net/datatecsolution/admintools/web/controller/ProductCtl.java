package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.Product;
import net.datatecsolution.admintools.domain.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = { "http://201.190.38.238", "http://localhost:3000/" })
public class ProductCtl {
    @Autowired
    private ProductService productService;

    // @GetMapping("/all")
    // public ResponseEntity<List<Product>> getAll() {
    //
    // return new ResponseEntity<>(productService.getAll(), HttpStatus.OK);
    // }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable("productId") int productId) {
        return productService.getProduct(productId)
                .map(product -> new ResponseEntity<>(product, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/description/{description}")
    public ResponseEntity<List<Product>> getByDescription(@PathVariable("description") String description,
            @RequestParam String user) {
        // System.out.println("Descripcion : " + description);
        // System.out.println("user: " + user);
        if (description == null || description.isEmpty() || user == null || user.isEmpty()) {
            String errorMessage = "Error: Ambos parámetros 'busqueda' y 'usuario' son requeridos.";
            new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
        }
        return productService.getProductsPrecioUser(description, user)
                .map(product -> new ResponseEntity<>(product, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getByCategory(@PathVariable("categoryId") int categoryID) {
        return productService.getBycategory(categoryID)
                .map(products -> new ResponseEntity<>(products, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/despriciouser/{description}")
    public ResponseEntity<List<Product>> getByDesPrecioUser(@PathVariable("description") String description,
            @RequestParam String user) {

        if (description == null || description.isEmpty() || user == null || user.isEmpty()) {
            String errorMessage = "Error: Ambos parámetros 'busqueda' y 'usuario' son requeridos.";
            new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
        }
        return productService.getProductsPrecioUser(description, user)
                .map(product -> new ResponseEntity<>(product, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/save")
    public ResponseEntity<Product> save(@RequestBody Product product) {

        return new ResponseEntity<>(productService.save(product), HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity delete(@PathVariable("id") int productId) {
        if (productService.delete(productId)) {
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }
}
