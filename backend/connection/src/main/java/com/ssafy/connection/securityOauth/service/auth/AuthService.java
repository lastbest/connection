package com.ssafy.connection.securityOauth.service.auth;

import com.ssafy.connection.dto.ResponseDto;
import com.ssafy.connection.dto.SolvedacUserDto;
import com.ssafy.connection.dto.StudyReadmeDto;
import com.ssafy.connection.entity.*;
import com.ssafy.connection.repository.*;
import com.ssafy.connection.securityOauth.advice.assertThat.DefaultAssert;
import com.ssafy.connection.securityOauth.config.security.token.UserPrincipal;
import com.ssafy.connection.securityOauth.domain.entity.user.*;
import com.ssafy.connection.securityOauth.domain.mapping.TokenMapping;
import com.ssafy.connection.securityOauth.payload.request.auth.ChangePasswordRequest;
import com.ssafy.connection.securityOauth.payload.request.auth.RefreshTokenRequest;
import com.ssafy.connection.securityOauth.payload.request.auth.SignInRequest;
import com.ssafy.connection.securityOauth.payload.request.auth.SignUpRequest;
import com.ssafy.connection.securityOauth.payload.response.ApiResponse;
import com.ssafy.connection.securityOauth.payload.response.AuthResponse;
import com.ssafy.connection.securityOauth.payload.response.Message;
import com.ssafy.connection.securityOauth.repository.auth.TokenRepository;
import com.ssafy.connection.securityOauth.repository.user.UserRepository;
import com.ssafy.connection.service.OrganizationService;
import com.ssafy.connection.service.StudyServiceImpl;
import com.ssafy.connection.util.ModelMapperUtils;
import lombok.RequiredArgsConstructor;
import org.hibernate.jdbc.Work;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.convention.NameTokenizers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.persistence.*;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final CustomTokenProviderService customTokenProviderService;
    private final StudyServiceImpl studyService;
    
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final ConnStudyRepository connStudyRepository;
    private final StudyRepository studyRepository;
    private final WorkbookRepository workbookRepository;
    private final SubjectRepository subjectRepository;
    private final SolveRepository solveRepository;
    private final ConnWorkbookRepository connWorkbookRepository;
    private final ReviewRepository reviewRepository;
    private final OrganizationService organizationService;
    private final String adminGithubToken = "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";
    private WebClient webClient = WebClient.create("https://api.github.com");
    private WebClient solvedac = WebClient.create("https://solved.ac/api");
    private WebClient github = WebClient.create("https://solved.ac/api");

    @Transactional
    public ResponseEntity whoAmI(UserPrincipal userPrincipal){
        Optional<User> user = userRepository.findById(userPrincipal.getId());
        UserDto userDto = ModelMapperUtils.getModelMapper().map(user.get(), UserDto.class);

        DefaultAssert.isOptionalPresent(user);
//        ApiResponse apiResponse = ApiResponse.builder().information(userDto).build();

//        return ResponseEntity.ok(userDto);
        return new ResponseEntity(userDto,HttpStatus.OK);
    }

    public ResponseEntity getAuthBoj(String code, String baekjoonId, long userId){
        SolvedacUserDto solvedacUserDto = new SolvedacUserDto();
        try {
            solvedacUserDto = solvedac.get()
                    .uri("/v3/user/show?handle={baekjoonId}", baekjoonId)
                    .retrieve()
                    .bodyToMono(SolvedacUserDto.class)
                    .block();
        }
        catch (Exception e){
            return new ResponseEntity(new ResponseDto("empty"), HttpStatus.CONFLICT);
        }

        if(solvedacUserDto.getBio().length() >= code.length()){ //?????????????????? ???????????? ?????? ??? ???
            String substr = solvedacUserDto.getBio().substring(solvedacUserDto.getBio().length()-code.length());
            if(substr.equals(code)){
//                User u = userRepository.findById(userId).get();
//                User user = new User();
//                user.setUserId(u.getUserId());
//                user.setName(u.getName());
//                user.setGithubId(u.getGithubId());
//                user.setEmail(u.getEmail());
//                user.setImageUrl(u.getImageUrl());
//                user.setTier(u.getTier());
//                user.setIsmember(u.isIsmember());
//                user.setPassword(u.getPassword());
//                user.setProvider(u.getProvider());
//                user.setRole(u.getRole());
//                user.setConnStudy(u.getConnStudy());
//                user.setSolve(u.getSolve());
//                user.setBackjoonId(baekjoonId);
//
//                userRepository.save(user);
                return new ResponseEntity(new ResponseDto("success"), HttpStatus.OK);
            }
        }

        return new ResponseEntity(new ResponseDto("fail"), HttpStatus.CONFLICT);
    }

    private boolean valid(String refreshToken){

        //1. ?????? ?????? ????????? ??????
        boolean validateCheck = customTokenProviderService.validateToken(refreshToken);
        DefaultAssert.isTrue(validateCheck, "Token ????????? ?????????????????????.");

        //2. refresh token ?????? ????????????.
        Optional<Token> token = tokenRepository.findByRefreshToken(refreshToken);
        DefaultAssert.isTrue(token.isPresent(), "?????? ????????? ???????????????.");

        //3. email ?????? ?????? ???????????? ????????????
        Authentication authentication = customTokenProviderService.getAuthenticationByGithubId(token.get().getGithubId());
        DefaultAssert.isTrue(token.get().getGithubId().equals(authentication.getName()), "????????? ????????? ?????????????????????.");//dd???????????????????????????????????????????????????

        return true;
    }

    @Transactional
    public ResponseEntity<?> delete(UserPrincipal userPrincipal){
        Optional<User> user = userRepository.findById(userPrincipal.getId());
        DefaultAssert.isTrue(user.isPresent(), "????????? ???????????? ????????????.");
        Optional<Token> token = tokenRepository.findByGithubId(user.get().getGithubId());
        DefaultAssert.isTrue(token.isPresent(), "????????? ???????????? ????????????.");

        if(user.isPresent()) {
            Optional<ConnStudy> connStudy = connStudyRepository.findByUser_UserId(userPrincipal.getId()); // ????????? ???????????????
            Optional<List<Solve>> solveList = solveRepository.findAllByUser(user.get());

            if (solveList.isPresent()) { // ??? ????????? ?????????
                solveRepository.deleteAllByUser(user.get());
            }

            if(connStudy.isPresent()) { // ????????? ???????????????

                if (connStudy.get().getRole().equals("LEADER")) { // ?????????????????? ????????? ?????? ?????? ??????
                    studyService.deleteStudy(user.get().getUserId()); // ????????? ?????? ??????(?????????, ?????????, ??????, ????????????)
                }
                else {
                    try {
                        studyService.updateStudyReadme(StudyReadmeDto.builder()
                                .studyId(connStudy.get().getStudy().getStudyId())
                                .msg(user.get().getGithubId() + "has quit <connection/>")
                                .build());
                    }
                    catch (Exception e){}

                    Study study = connStudy.get().getStudy();
                    connStudyRepository.delete(connStudy.get()); // ?????????????????? ???????????? ?????? ??????

                    long studyPersonnel = connStudyRepository.countByStudy_StudyId(study.getStudyId());
                    study.setStudyPersonnel((int)studyPersonnel);
                    studyRepository.save(study);
                }
            }

            reviewRepository.deleteAllByUser(user.get());

            webClient.delete()
                    .uri("/orgs/{org}/memberships/{username}",
                            "connection-official",
                            user.get().getGithubId())
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + adminGithubToken)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            userRepository.delete(user.get());
            tokenRepository.delete(token.get());
        }

        ApiResponse apiResponse = ApiResponse.builder().check(true).information(Message.builder().message("?????? ?????????????????????.").build()).build();

        return ResponseEntity.ok(apiResponse);
    }

    @Transactional
    public ResponseEntity joinOrQuitOrganization(String githubId, boolean joq){
        Optional<User> optionalUser = userRepository.findByGithubId(githubId);
        if(!optionalUser.isPresent()) return new ResponseEntity(new ResponseDto("empty"), HttpStatus.CONFLICT);

        User u = optionalUser.get();
        User user = new User();
        user.setUserId(u.getUserId());
        user.setName(u.getName());
        user.setGithubId(u.getGithubId());
        user.setBackjoonId(u.getBackjoonId());
        user.setEmail(u.getEmail());
        user.setImageUrl(u.getImageUrl());
        user.setTier(u.getTier());
        user.setIsmember(joq);
        user.setPassword(u.getPassword());
        user.setProvider(u.getProvider());
        user.setRole(u.getRole());
        user.setConnStudy(u.getConnStudy());
        user.setSolve(u.getSolve());

        userRepository.save(user);

        if(!joq){
            try {
                organizationService.joinOrganization(user.getUserId());
            }
            catch (Exception e) {
                System.out.println("????????????");
            }
        }

        return ResponseEntity.ok(new ResponseDto("success"));
    }

//    public ResponseEntity<?> modify(UserPrincipal userPrincipal, ChangePasswordRequest passwordChangeRequest){
//        Optional<User> user = userRepository.findById(userPrincipal.getId());
//        boolean passwordCheck = passwordEncoder.matches(passwordChangeRequest.getOldPassword(),user.get().getPassword());
//        DefaultAssert.isTrue(passwordCheck, "????????? ???????????? ?????????.");
//
//        boolean newPasswordCheck = passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getReNewPassword());
//        DefaultAssert.isTrue(newPasswordCheck, "?????? ?????? ???????????? ?????? ???????????? ????????????.");
//
//
//        passwordEncoder.encode(passwordChangeRequest.getNewPassword());
//
//        return ResponseEntity.ok(true);
//    }
//
//    public ResponseEntity<?> signin(SignInRequest signInRequest){
//        Authentication authentication = authenticationManager.authenticate(
//            new UsernamePasswordAuthenticationToken(
//                signInRequest.getEmail(),
//                signInRequest.getPassword()
//            )
//        );
//
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        TokenMapping tokenMapping = customTokenProviderService.createToken(authentication);
//        Token token = Token.builder()
//                            .refreshToken(tokenMapping.getRefreshToken())
//                            .githubId(tokenMapping.getGithubId())
//                            .build();
//        tokenRepository.save(token);
//        AuthResponse authResponse = AuthResponse.builder().accessToken(tokenMapping.getAccessToken()).refreshToken(token.getRefreshToken()).build();
//
//        return ResponseEntity.ok(authResponse);
//    }
//
//    public ResponseEntity<?> signup(SignUpRequest signUpRequest){
//        DefaultAssert.isTrue(!userRepository.existsByEmail(signUpRequest.getEmail()), "?????? ???????????? ???????????? ????????????.");
//
//        User user = User.builder()
//                        .name(signUpRequest.getName())
//                        .email(signUpRequest.getEmail())
//                        .password(passwordEncoder.encode(signUpRequest.getPassword()))
//                        .provider(Provider.local)
//                        .role(Role.ADMIN)
//                        .build();
//
//        userRepository.save(user);
//
//        URI location = ServletUriComponentsBuilder
//                .fromCurrentContextPath().path("/auth/")
//                .buildAndExpand(user.getUserId()).toUri();
//        ApiResponse apiResponse = ApiResponse.builder().check(true).information(Message.builder().message("??????????????? ?????????????????????.").build()).build();
//
//        return ResponseEntity.created(location).body(apiResponse);
//    }

    public ResponseEntity<?> refresh(String refreshToken){
        //1??? ??????
        boolean checkValid = valid(refreshToken);
        System.out.println("refresh??????");
        DefaultAssert.isAuthentication(checkValid);

        Optional<Token> token = tokenRepository.findByRefreshToken(refreshToken);
        Authentication authentication = customTokenProviderService.getAuthenticationByGithubId(token.get().getGithubId());

        //4. refresh token ?????? ?????? ???????????? ??????.
        //?????? ????????? ??????
        TokenMapping tokenMapping;

        Long expirationTime = customTokenProviderService.getExpiration(refreshToken);
        if(expirationTime > 0){
            tokenMapping = customTokenProviderService.refreshToken(authentication, token.get().getRefreshToken());
        }else{
            tokenMapping = customTokenProviderService.createToken(authentication);
        }

        Token updateToken = token.get().updateRefreshToken(tokenMapping.getRefreshToken());
        tokenRepository.save(updateToken);

        AuthResponse authResponse = AuthResponse.builder().accessToken(tokenMapping.getAccessToken()).refreshToken(updateToken.getRefreshToken()).build();

        return ResponseEntity.ok(authResponse);
    }

    public ResponseEntity<?> signout(RefreshTokenRequest tokenRefreshRequest){
        boolean checkValid = valid(tokenRefreshRequest.getRefreshToken());
        System.out.println("signout??????");
        DefaultAssert.isAuthentication(checkValid);

        //4 token ????????? ????????????.
        Optional<Token> token = tokenRepository.findByRefreshToken(tokenRefreshRequest.getRefreshToken());
        tokenRepository.delete(token.get());
        ApiResponse apiResponse = ApiResponse.builder().check(true).information(Message.builder().message("???????????? ???????????????.").build()).build();

        return ResponseEntity.ok(apiResponse);
    }




}
