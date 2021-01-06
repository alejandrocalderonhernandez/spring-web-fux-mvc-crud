package com.alejandro.webflux.controller;

import java.time.Duration;
import java.time.LocalDate;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import com.alejandro.webflux.document.ProductDocument;
import com.alejandro.webflux.service.ProductService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class ProductController {
	
	private static final String TITLE_VIEW = "title";
	private static final String ATTR_NAME = "product";
	
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(ProductController.class);
	@Autowired
	private ProductService service;
	
	@GetMapping(value = {"/toList", "/"})
	public Mono<String> toList(Model model) {
		Flux<ProductDocument> response = this.service.findAll(); 
		model.addAttribute("products", response);
		model.addAttribute(TITLE_VIEW, "Products");
		return Mono.just("toList");
	}
	
	@GetMapping(value = "/dataDriver")
	public String toListDataDriver(Model model) {
		Flux<ProductDocument> response = this.service.findAll()
				.delayElements(Duration.ofSeconds(1)); 
		ReactiveDataDriverContextVariable compresionResponse = 
				new ReactiveDataDriverContextVariable(response, 1);
		model.addAttribute("products", compresionResponse);
		model.addAttribute(TITLE_VIEW, "Products");
		return "toList";
	}
	
	@GetMapping(value = "/form")
	public Mono<String> create(Model model) {
		model.addAttribute(ATTR_NAME, new ProductDocument());
		model.addAttribute(TITLE_VIEW, "Form product");
		return Mono.just("form");
	}
	
	@GetMapping(value = "/form/{id}")
	public Mono<String> edit(@PathVariable String id, Model model) {
		Mono<ProductDocument> product = this.service.findById(id).doOnNext(p -> {
			log.info(p.getName());
		});
		model.addAttribute(ATTR_NAME, product);
		model.addAttribute(TITLE_VIEW, "Edit product");
		return Mono.just("form");
	}
	
	@GetMapping(value = "/delete/{id}")
	public Mono<String> delete(@PathVariable String id, Model model) {
		Mono<ProductDocument> response = this.service.findById(id);
		response.subscribe(p -> {
			if(p == null ) {
				throw new IllegalArgumentException();
			}
		});
		return response.flatMap(p -> {
			return this.service.delete(p);
		}).then(Mono.just("redirect:/toList"));
	}
	
	@PostMapping(value = "/form")
	public Mono<String> save(@Valid ProductDocument product,
			BindingResult bindingResult, Model model) {
		System.out.println(bindingResult.hasErrors());
		if(bindingResult.hasErrors()) {
			model.addAttribute(TITLE_VIEW, "Errors");
			model.addAttribute("button", "Save");
			return Mono.just("form");
		} else {
			if(product.getDate() == null) {
				product.setDate(LocalDate.now());
			}
			return this.service.save(product)
					.doOnNext(p -> log.info("saved: " + p.getName()))
					.thenReturn("redirect:/toList");
		}
	
	}

}
