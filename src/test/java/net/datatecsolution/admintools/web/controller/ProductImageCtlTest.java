package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import net.datatecsolution.admintools.domain.service.ProductImageService;
import net.datatecsolution.admintools.persistence.entity.ArticuloImagen;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-079 — slice del controller de imagen de producto.
 */
@WebMvcTest(controllers = ProductImageCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductImageCtlTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductImageService service;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean(name = "commonDataSource") private DataSource commonDataSource;

    private ArticuloImagen imagen() {
        ArticuloImagen img = new ArticuloImagen();
        img.setIdImg(41);
        img.setCodigoArticulo(7);
        img.setImg(new byte[]{1, 2, 3});
        img.setExtension("jpg");
        return img;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void upload_retorna201ConImageVersion() throws Exception {
        when(service.store(eq(7), any())).thenReturn(41);

        mockMvc.perform(multipart("/products/7/image")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageVersion").value(41));
    }

    @Test
    @WithMockUser
    void get_devuelveBytesConEtagYCacheInmutable() throws Exception {
        when(service.get(7)).thenReturn(Optional.of(imagen()));

        mockMvc.perform(get("/products/7/image"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"41\""))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("immutable")))
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    @WithMockUser
    void get_conIfNoneMatchCoincidente_304() throws Exception {
        when(service.get(7)).thenReturn(Optional.of(imagen()));

        mockMvc.perform(get("/products/7/image").header("If-None-Match", "\"41\""))
                .andExpect(status().isNotModified());
    }

    @Test
    @WithMockUser
    void get_sinImagen_404() throws Exception {
        when(service.get(7)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/7/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_retorna204() throws Exception {
        mockMvc.perform(delete("/products/7/image"))
                .andExpect(status().isNoContent());

        verify(service).delete(7);
    }
}
