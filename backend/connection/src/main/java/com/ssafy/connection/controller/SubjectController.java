package com.ssafy.connection.controller;

import com.ssafy.connection.dto.ResponseDto;
import com.ssafy.connection.dto.SubjectDto;
import com.ssafy.connection.entity.Problem;
import com.ssafy.connection.entity.Subject;
import com.ssafy.connection.entity.Tag;
import com.ssafy.connection.securityOauth.config.security.token.CurrentUser;
import com.ssafy.connection.securityOauth.config.security.token.UserPrincipal;
import com.ssafy.connection.service.ProblemService;
import com.ssafy.connection.service.SubjectService;
import com.ssafy.connection.service.TagService;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/subject")
public class SubjectController {

    private final ProblemService problemService;
    private final SubjectService subjectService;

    @Autowired
    public SubjectController(ProblemService problemService, SubjectService subjectService){
        this.problemService = problemService;
        this.subjectService = subjectService;
    }

    @ApiOperation(value = "과제 출제")
    @ApiResponse(responseCode = "200", description = "success : 성공")
    @ApiResponse(responseCode = "409", description = "empty : 해당회원 스터디 정보 없음<br>" +
                                                    "wrong parameter value : 해당문제 존재하지 않거나 데드라인 잘못됨")
//    @ApiResponse(responseCode = "409", description = "성공")
    @PostMapping("")
    public ResponseEntity makeSubject(@RequestBody SubjectDto subjectDto, @Parameter(description = "Accesstoken", required = true) @CurrentUser UserPrincipal userPrincipal){
        if(subjectDto.getDeadline() == null) return new ResponseEntity<>(new ResponseDto("wrong parameter value"), HttpStatus.CONFLICT);
        ResponseEntity result = null;
        try{
            Long userId = userPrincipal.getId();
            result = subjectService.makeSubject(subjectDto, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @ApiOperation(value = "팀 과제 현황", notes = "")
    @ApiResponse(responseCode = "200", description = "success : 성공<br>" +
            "example : <br>{<br>" +
            "&nbsp;\"inProgress\": false,<br>" +
            "&nbsp;\"subjects\": [<br>" +
            "&nbsp;&nbsp;{<br>" +
            "&nbsp;&nbsp;&nbsp;\"deadline\": [<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;\"2022-10-31\",<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;\"2022-11-02\"<br>" +
            "&nbsp;&nbsp;&nbsp;],<br>" +
            "&nbsp;&nbsp;&nbsp;\"users\": [<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;{<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"problem_cnt\": 1,<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"user_id\": 7,<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"user_name\": \"Connection-code\"<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;},<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;...<br>" +
            "&nbsp;&nbsp;&nbsp;],<br>" +
            "&nbsp;&nbsp;&nbsp;\"problems\": [<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;{<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"problem_id\": 130,<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"problem_name\": \"문제제목\",<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"problem_solved\": [<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true,<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;false,<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;false<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;]<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;},<br>" +
            "&nbsp;&nbsp;&nbsp;]<br>" +
            "&nbsp;&nbsp;},<br>" +
            "&nbsp;]<br>" +
            "}<br>")
    @GetMapping("/team")
    public ResponseEntity getTeamStatus(@Parameter(description = "Accesstoken", required = true) @CurrentUser UserPrincipal userPrincipal){
        return subjectService.getTeamStatus(userPrincipal.getId());
    }

    @ApiOperation(value = "내 과제 현황", notes = "유저가 푼 과제 개수, 스터디문제(같이 푼) 개수와 전체 과제개수, 전체 스터디문제 개수를 반환")
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyStatus(@Parameter(description = "Accesstoken", required = true) @CurrentUser UserPrincipal userPrincipal){
        Map<String, Object> returnMap = new HashMap<>();
        List<Subject> totalSubjectList = subjectService.getTotalSubjectList(userPrincipal.getId());
        Map<String, Object> myMap = subjectService.getMyStatus(userPrincipal.getId(), totalSubjectList);

        returnMap.put("msg", "success");
        returnMap.put("data", myMap);

        return ResponseEntity.status(HttpStatus.OK).body(returnMap);
    }
}