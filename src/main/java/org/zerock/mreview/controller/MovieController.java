package org.zerock.mreview.controller;

import com.sun.org.apache.xpath.internal.operations.Mod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.zerock.mreview.dto.MovieDTO;
import org.zerock.mreview.dto.MovieImageDTO;
import org.zerock.mreview.dto.PageRequestDTO;
import org.zerock.mreview.service.MovieService;

import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/movie")
@Log4j2
@RequiredArgsConstructor
public class MovieController {

    @Value("${org.zerock.upload.path}")
    private String uploadPath;

    private final MovieService movieService;

    @GetMapping("/register")
    public void register(){

    }

    @PostMapping("/register")
    public String register(MovieDTO movieDTO, RedirectAttributes redirectAttributes){
        log.info("movieDTO: " + movieDTO);

        Long mno = movieService.register(movieDTO);

        redirectAttributes.addFlashAttribute("msg", mno);

        return "redirect:/movie/list";
    }

    @GetMapping("/list")
    public void list(PageRequestDTO pageRequestDTO, Model model){
        log.info("pageRequestDTO: " + pageRequestDTO);

        model.addAttribute("result", movieService.getList(pageRequestDTO));
    }

    @GetMapping({"/read", "/modify"})
    public void read(Long mno, @ModelAttribute("requestDTO") PageRequestDTO requestDTO, Model model){
        log.info("mno: " + mno);

        MovieDTO movieDTO = movieService.getMovie(mno);

        model.addAttribute("dto", movieDTO);
    }

    @PostMapping("/remove")
    public String remove(Long mno, RedirectAttributes redirectAttributes){

        List<MovieImageDTO> movieImageDTOList = movieService.getMovie(mno).getImageDTOList();

        // delete review, image, movie data
        movieService.remove(mno);

        // delete file
        deleteFiles(movieImageDTOList);
        
        redirectAttributes.addFlashAttribute("msg", mno);

        return "redirect:/movie/list";

    }

    private void deleteFiles(List<MovieImageDTO> imageDTOList){


        if(imageDTOList==null || imageDTOList.size()==0){
            return;
        }

        // delete original file, thumbnail file
        imageDTOList.forEach(movieImageDTO -> {
            try {

                String fileName = uploadPath + "/" + URLDecoder.decode(movieImageDTO.getImageURL(), "UTF-8");
                Path path = Paths.get(fileName);
                Files.deleteIfExists(path);

                fileName = uploadPath + "/" + URLDecoder.decode(movieImageDTO.getThumbnailURL(), "UTF-8");
                path = Paths.get(fileName);
                Files.deleteIfExists(path);

            } catch (Exception e){
                log.error("delete file error: " + e.getMessage());
            }
        });
    }


    @PostMapping("/modify")
    public String modify(MovieDTO movieDTO, @ModelAttribute("requestDTO")PageRequestDTO requestDTO,
                         RedirectAttributes redirectAttributes){

        movieService.modify(movieDTO);

        // 수정된 조회화면으로
        redirectAttributes.addAttribute("page", requestDTO.getPage());
        redirectAttributes.addAttribute("type", requestDTO.getType());
        redirectAttributes.addAttribute("keyword", requestDTO.getKeyword());
        redirectAttributes.addAttribute("mno", movieDTO.getMno());

        return "redirect:/movie/read";
    }

    // 등록화면에서 사진 첨부해놓고 등록 안하고 그냥 뒤로가기나 껐을때 사진 삭제 처리
}
