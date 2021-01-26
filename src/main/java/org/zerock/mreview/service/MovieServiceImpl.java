package org.zerock.mreview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.mreview.dto.MovieDTO;
import org.zerock.mreview.dto.MovieImageDTO;
import org.zerock.mreview.dto.PageRequestDTO;
import org.zerock.mreview.dto.PageResultDTO;
import org.zerock.mreview.entity.Movie;
import org.zerock.mreview.entity.MovieImage;
import org.zerock.mreview.entity.Review;
import org.zerock.mreview.repository.MovieImageRepository;
import org.zerock.mreview.repository.MovieRepository;
import org.zerock.mreview.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@Log4j2
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService{

    private final MovieRepository movieRepository;
    private final MovieImageRepository imageRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    @Override
    public Long register(MovieDTO movieDTO) {

        Map<String, Object> entityMap = dtoToEntity(movieDTO);
        Movie movie = (Movie)entityMap.get("movie");
        List<MovieImage> movieImageList= (List<MovieImage>)entityMap.get("imgList");

        movieRepository.save(movie);
        movieImageList.forEach(movieImage -> {
            imageRepository.save(movieImage);
        });

        return movie.getMno();
    }

    @Override
    public PageResultDTO<MovieDTO, Object[]> getList(PageRequestDTO requestDTO) {

        Pageable pageable = requestDTO.getPageable(Sort.by("mno").descending());

        Page<Object[]> result = movieRepository.getListPage(pageable);

        Function<Object[], MovieDTO> fn = (arr -> entitiesToDTO(
                (Movie)arr[0],
                (List<MovieImage>) (Arrays.asList((MovieImage)arr[1])),
                (Double)arr[2],
                (Long)arr[3])
        );

        return new PageResultDTO<>(result, fn);
    }

    @Override
    public MovieDTO getMovie(Long mno) {

        List<Object[]> result = movieRepository.getMovieWithAll(mno);

        Movie movie = (Movie)result.get(0)[0];

        List<MovieImage> movieImageList = new ArrayList<>();

        result.forEach(arr -> {
            MovieImage movieImage = (MovieImage) arr[1];
            movieImageList.add(movieImage);
        });

        Double avg = (Double)result.get(0)[2];
        Long reviewCnt = (Long)result.get(0)[3];

        return entitiesToDTO(movie, movieImageList, avg, reviewCnt);
    }

    @Override
    @Transactional
    public void remove(Long mno) {

        reviewRepository.deleteByMno(mno);
        imageRepository.deleteByMno(mno);
        movieRepository.deleteById(mno);
    }

    @Transactional
    @Override
    public Long modify(MovieDTO movieDTO) {

        List<Object[]> result = movieRepository.getMovieWithAll(movieDTO.getMno());

        Movie movie = (Movie)result.get(0)[0];

        /*List<MovieImage> movieImageList = new ArrayList<>();
        result.forEach(movieImage -> {
            movieImageList.add((MovieImage) movieImage[1]);
        });*/

        movie.changeTitle(movieDTO.getTitle());

        imageRepository.deleteByMno(movie.getMno());

        List<MovieImageDTO> movieImageDTOList = movieDTO.getImageDTOList();

        movieImageDTOList.forEach(movieImageDTO->{
            MovieImage movieImage = MovieImage.builder()
                    .path(movieImageDTO.getPath())
                    .imgName(movieImageDTO.getImgName())
                    .uuid(movieImageDTO.getUuid())
                    .movie(movie)
                    .build();
            imageRepository.save(movieImage);
        });
        movieRepository.save(movie);

        return movie.getMno();
    }
}
