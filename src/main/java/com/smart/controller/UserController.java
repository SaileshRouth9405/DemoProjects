package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepo;
import com.smart.dao.UserRepo;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private ContactRepo contactRepo;
	
	//method for adding  common data to response
	@ModelAttribute
	public void addCommonData(Model m,Principal p)
	{
		String userName=p.getName();
		User user=userRepo.getUserByUserName(userName);
		m.addAttribute("user",user);
	}
	
	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model m)
	{
		m.addAttribute("title","User DashBoard");
		return "normal/user_dashboard";
	}
	
	//Open and Add Handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model m)
	{
		m.addAttribute("title","Add Contact");
		m.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@Valid @ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,BindingResult result, Principal p,HttpSession hts,Model m)
	{
		
		try {
			String name=p.getName();
			User user=this.userRepo.getUserByUserName(name);
			contact.setUser(user);
			user.getContacts().add(contact);
			//processing and uploading file
			
			if(file.isEmpty())
			{
				contact.setImage("contact.png");
			}
			else
			{
			contact.setImage(file.getOriginalFilename());
			File saveFile=new ClassPathResource("static/img").getFile();
			Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			
			this.userRepo.save(user);
			System.out.println("Added to db");
			m.addAttribute("title","Add Contact");
			hts.setAttribute("message",new Message("Your contact is added !! Add new one","success"));
			
		}
		catch (Exception e) {
			m.addAttribute("title","Add Contact");
			System.out.println("ERROR"+e.getMessage());
			e.printStackTrace();
			hts.setAttribute("message",new Message("Something went Wrong !! Try Again...","danger"));
		}
		return "normal/add_contact_form";
	}
	//show contact handler
	//per page = 5[n]
	//current page = 0 [page]
	@GetMapping("/show-contact/{page}")
	public String showContact(@PathVariable("page") Integer  page,Model m,Principal p)
	{
		m.addAttribute("title","Show Contacts");
		String userName=p.getName();
		User user=userRepo.getUserByUserName(userName);
		Pageable pageable=PageRequest.of(page, 5);
		Page<Contact> contacts=contactRepo.findContactByUser(user.getId(),pageable);
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//showing particular contact details
	@RequestMapping("/contact/{cId}")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model m,Principal principal)
	{
		Optional<Contact> contactOptional=this.contactRepo.findById(cId);
		Contact contact=contactOptional.get();
		
		String userName=principal.getName();
		User user=userRepo.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId())
		{
 			m.addAttribute("contact",contact);
 			m.addAttribute("title",contact.getName());
		}
		
		return "normal/contact_detail";
	}
	
	//delete contact handler
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId,Model m,Principal principal,HttpSession session)
	{
	   Optional<Contact> contactOptional=contactRepo.findById(cId);
	   Contact contact=contactOptional.get();
	   
//	   //check
//	   String userName=principal.getName();
//		User user=userRepo.getUserByUserName(userName);
//
//		if(user.getId()==contact.getUser().getId())
//		{
//			contact.setUser(null);
//			 contactRepo.delete(contact);
//		}
		
	   User user=userRepo.getUserByUserName(principal.getName());
	   user.getContacts().remove(contact);
	   
	   userRepo.save(user);
	
		session.setAttribute("message",new Message("Contact Deleted Successfully...","success"));
	  
		return "redirect:/user/show-contact/0";
	}
	
	//open update from handler
	@PostMapping("/update-contact/{cId}")
	public String updateContact(Model m,@PathVariable("cId") Integer cId)
	{
		m.addAttribute("title","Update Contact");
		
		Contact contact=contactRepo.findById(cId).get();
		m.addAttribute("contact",contact);
		
		return "normal/update_contact";
	}
	
	//update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model m,HttpSession session,Principal principal)
	{
		try {
			Contact oldContactDetails=contactRepo.findById(contact.getcId()).get();
			if(!file.isEmpty())
			{
				
				File deleteFile=new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldContactDetails.getImage());
				file1.delete();
				
				File saveFile=new ClassPathResource("static/img").getFile();
				Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			}
			else
			{
				contact.setImage(oldContactDetails.getImage());
			}
			
			User user=userRepo.getUserByUserName(principal.getName());
			contact.setUser(user);
			contactRepo.save(contact);
			
			session.setAttribute("message", new Message("Your Contact is Updated...","success" ));
		} catch (Exception e) {
				e.printStackTrace();
		}
		return "redirect:/user/contact/"+contact.getcId();
	}
	
	//Your Profile Handler
	@GetMapping("/profile")
	public String yourprofile(Model m)
	{
		m.addAttribute("title","Profile Page");
		return "normal/profile";
	}
}
