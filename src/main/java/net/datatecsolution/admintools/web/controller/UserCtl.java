package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.User;
import net.datatecsolution.admintools.domain.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserCtl {
    @Autowired
    private UserService userService;

//    @GetMapping("/all")
//    public List<User> getAll() {
//        return userService.getAll();
//    }
}
