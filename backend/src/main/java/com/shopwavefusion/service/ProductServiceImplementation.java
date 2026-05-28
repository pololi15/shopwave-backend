package com.shopwavefusion.service;


import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.shopwavefusion.exception.ProductException;
import com.shopwavefusion.modal.Category;
import com.shopwavefusion.modal.Product;
import com.shopwavefusion.repository.CategoryRepository;
import com.shopwavefusion.repository.ProductRepository;
import com.shopwavefusion.request.CreateProductRequest;

import jakarta.transaction.Transactional;

@Service
public class ProductServiceImplementation implements ProductService {


	private ProductRepository productRepository;
	private UserService userService;
	private CategoryRepository categoryRepository;
	
	public ProductServiceImplementation(ProductRepository productRepository,UserService userService,CategoryRepository categoryRepository) {
		this.productRepository=productRepository;
		this.userService=userService;
		this.categoryRepository=categoryRepository;
	}
	@Override
    public Page<Product> getProductsSortedByDiscountedPrice(String sortDirection, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);

        if ("asc".equalsIgnoreCase(sortDirection)) {
            return productRepository.findAllByOrderByDiscountedPriceAsc(pageable);
        } else if ("desc".equalsIgnoreCase(sortDirection)) {
            return productRepository.findAllByOrderByDiscountedPriceDesc(pageable);
        } else {
            return productRepository.findAllByOrderByDiscountedPriceAsc(pageable);
        }
    }
	 @Override
	    public Page<Product> getProductsByCategory(String categoryName, int page, int pageSize) {
	        Pageable pageable = PageRequest.of(page, pageSize);
	        return productRepository.findByCategoryNameIgnoreCase(categoryName, pageable);
	    }
	 @Override
	    public Page<Product> getProductsByCategoryAndPriceRange(
	            String categoryName, Integer minPrice, Integer maxPrice, int page, int pageSize) {
	        Pageable pageable = PageRequest.of(page, pageSize);
	        return productRepository.findByCategoryNameIgnoreCaseAndDiscountedPriceBetween(
	                categoryName, minPrice, maxPrice, pageable);
	    }
	@Override
	@Transactional
	public Product createProduct(CreateProductRequest req) throws SQLException {
		System.out.println("reached here");
		Category topLevel=categoryRepository.findByName(req.getTopLevelCategory());
		
		if(topLevel==null) {
			
			Category topLavelCategory=new Category();
			topLavelCategory.setName(req.getTopLevelCategory());
			topLavelCategory.setLevel(1);
			System.out.println("topLeve category creating");
			topLevel= categoryRepository.save(topLavelCategory);
			
			System.out.println("topLeve category created");
		}
		
		Category secondLevel=categoryRepository.
				findByNameAndParant(req.getSecondLevelCategory(),topLevel.getName());
		if(secondLevel==null) {
			
			Category secondLavelCategory=new Category();
			secondLavelCategory.setName(req.getSecondLevelCategory());
			secondLavelCategory.setParentCategory(topLevel);
			secondLavelCategory.setLevel(2);
			
			secondLevel= categoryRepository.save(secondLavelCategory);
			System.out.println("secondLevel category created");
		}

		Category thirdLevel=categoryRepository.findByNameAndParant(req.getThirdLevelCategory(),secondLevel.getName());
		if(thirdLevel==null) {
			
			Category thirdLavelCategory=new Category();
			thirdLavelCategory.setName(req.getThirdLevelCategory());
			thirdLavelCategory.setParentCategory(secondLevel);
			thirdLavelCategory.setLevel(3);
			
			thirdLevel=categoryRepository.save(thirdLavelCategory);
		}
		
		
		Product product=new Product();
		product.setTitle(req.getTitle());
		product.setColor(req.getColor());
		product.setDescription(req.getDescription());
		product.setDiscountedPrice(req.getDiscountedPrice());
		product.setDiscountPersent(req.getDiscountPersent());
		product.setImageUrl(req.getImageUrl());
		product.setBrand(req.getBrand());
		product.setPrice(req.getPrice());
		product.setSizes(req.getSize());
		product.setQuantity(req.getQuantity());
		product.setCategory(thirdLevel);
		product.setCreatedAt(LocalDateTime.now());
		
		Product savedProduct= productRepository.save(product);
		
		
		return savedProduct;
	}

	@Override
	public String deleteProduct(Long productId) throws ProductException {
		
		Product product=findProductById(productId);
		
		System.out.println("delete product "+product.getId()+" - "+productId);
		product.getSizes().clear();
//		productRepository.save(product);
//		product.getCategory().
		productRepository.delete(product);
		
		return "Product deleted Successfully";
	}

@Override
	public Product updateProduct(Long productId,Product req) throws ProductException {
		Product product=findProductById(productId);

		if(req.getTitle()!=null) {
			product.setTitle(req.getTitle());
		}
		if(req.getDescription()!=null) {
			product.setDescription(req.getDescription());
		}
		if(req.getPrice()!=0) {
			product.setPrice(req.getPrice());
		}
		if(req.getDiscountedPrice()!=0) {
			product.setDiscountedPrice(req.getDiscountedPrice());
		}
		if(req.getDiscountPersent()!=0) {
			product.setDiscountPersent(req.getDiscountPersent());
		}
		if(req.getQuantity()!=0) {
			product.setQuantity(req.getQuantity());
		}
		if(req.getBrand()!=null) {
			product.setBrand(req.getBrand());
		}
		if(req.getColor()!=null) {
			product.setColor(req.getColor());
		}
		if(req.getImageUrl()!=null) {
			product.setImageUrl(req.getImageUrl());
		}
		if(req.getSizes()!=null && !req.getSizes().isEmpty()) {
			product.setSizes(req.getSizes());
		}

		return productRepository.save(product);
	}

	public Product updateProduct(Long productId, CreateProductRequest req) throws ProductException {
		Product product=findProductById(productId);

		if(req.getTitle()!=null && !req.getTitle().isEmpty()) {
			product.setTitle(req.getTitle());
		}
		if(req.getDescription()!=null) {
			product.setDescription(req.getDescription());
		}
		if(req.getPrice()!=0) {
			product.setPrice(req.getPrice());
		}
		if(req.getDiscountedPrice()!=0) {
			product.setDiscountedPrice(req.getDiscountedPrice());
		}
		if(req.getDiscountPersent()!=0) {
			product.setDiscountPersent(req.getDiscountPersent());
		}
		if(req.getQuantity()!=0) {
			product.setQuantity(req.getQuantity());
		}
		if(req.getBrand()!=null) {
			product.setBrand(req.getBrand());
		}
		if(req.getColor()!=null) {
			product.setColor(req.getColor());
		}
		if(req.getImageUrl()!=null) {
			product.setImageUrl(req.getImageUrl());
		}
		if(req.getSize()!=null && !req.getSize().isEmpty()) {
			product.setSizes(req.getSize());
		}

		if(req.getTopLevelCategory()!=null && !req.getTopLevelCategory().isEmpty()) {
			Category topLevel=categoryRepository.findByName(req.getTopLevelCategory());

			if(topLevel==null) {
				topLevel=new Category();
				topLevel.setName(req.getTopLevelCategory());
				topLevel.setLevel(1);
				topLevel=categoryRepository.save(topLevel);
			}

			Category secondLevel=categoryRepository.findByNameAndParant(req.getSecondLevelCategory(),topLevel.getName());
			if(secondLevel==null && req.getSecondLevelCategory()!=null && !req.getSecondLevelCategory().isEmpty()) {
				secondLevel=new Category();
				secondLevel.setName(req.getSecondLevelCategory());
				secondLevel.setParentCategory(topLevel);
				secondLevel.setLevel(2);
				secondLevel=categoryRepository.save(secondLevel);
			}

			Category thirdLevel=null;
			if(secondLevel!=null && req.getThirdLevelCategory()!=null && !req.getThirdLevelCategory().isEmpty()) {
				thirdLevel=categoryRepository.findByNameAndParant(req.getThirdLevelCategory(),secondLevel.getName());
				if(thirdLevel==null) {
					thirdLevel=new Category();
					thirdLevel.setName(req.getThirdLevelCategory());
					thirdLevel.setParentCategory(secondLevel);
					thirdLevel.setLevel(3);
					thirdLevel=categoryRepository.save(thirdLevel);
				}
			}

			if(thirdLevel!=null) {
				product.setCategory(thirdLevel);
			} else if(secondLevel!=null) {
				product.setCategory(secondLevel);
			} else {
				product.setCategory(topLevel);
			}
		}

return productRepository.save(product);
	}

	@Override
	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}

	@Override
	public Product findProductById(Long id) throws ProductException {
		Optional<Product> opt=productRepository.findById(id);
		
		if(opt.isPresent()) {
			return opt.get();
		}
		throw new ProductException("product not found with id "+id);
	}

	@Override
	public List<Product> findProductByCategory(String category) {
		
		System.out.println("category --- "+category);
		
		List<Product> products = productRepository.findByCategory(category);
		
		return products;
	}

	@Override
	public List<Product> searchProduct(String query) {
		List<Product> products=productRepository.searchProduct(query);
		return products;
	}



	
	
	@Override
	public Page<Product> getAllProduct(String category, List<String>colors, 
			List<String> sizes, Integer minPrice, Integer maxPrice, 
			Integer minDiscount,String sort, String stock, Integer pageNumber, Integer pageSize ) {

		Pageable pageable = PageRequest.of(pageNumber, pageSize);
		
		List<Product> products = productRepository.filterProducts(category, minPrice, maxPrice, minDiscount, sort);
		
		
		if (!colors.isEmpty()) {
			products = products.stream()
			        .filter(p -> colors.stream().anyMatch(c -> c.equalsIgnoreCase(p.getColor())))
			        .collect(Collectors.toList());
		
		
		} 

		if(stock!=null) {

			if(stock.equals("in_stock")) {
				products=products.stream().filter(p->p.getQuantity()>0).collect(Collectors.toList());
			}
			else if (stock.equals("out_of_stock")) {
				products=products.stream().filter(p->p.getQuantity()<1).collect(Collectors.toList());				
			}
				
					
		}
		int startIndex = (int) pageable.getOffset();
		int endIndex = Math.min(startIndex + pageable.getPageSize(), products.size());

		List<Product> pageContent = products.subList(startIndex, endIndex);
		Page<Product> filteredProducts = new PageImpl<>(pageContent, pageable, products.size());
	    return filteredProducts; // If color list is empty, do nothing and return all products
		
		
	}

}
