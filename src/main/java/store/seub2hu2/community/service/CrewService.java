package store.seub2hu2.community.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import store.seub2hu2.community.dto.CrewForm;
import store.seub2hu2.community.exception.CommunityException;
import store.seub2hu2.community.mapper.CrewMapper;
import store.seub2hu2.community.mapper.CrewReplyMapper;
import store.seub2hu2.community.mapper.ReplyMapper;
import store.seub2hu2.community.mapper.UploadMapper;
import store.seub2hu2.community.vo.Crew;
import store.seub2hu2.community.vo.CrewMember;
import store.seub2hu2.community.vo.Reply;
import store.seub2hu2.community.vo.UploadFile;
import store.seub2hu2.security.user.LoginUser;
import store.seub2hu2.user.vo.User;
import store.seub2hu2.util.ListDto;
import store.seub2hu2.util.Pagination;
import store.seub2hu2.util.S3Service;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class CrewService {

    @Value("${upload.directory.crew.images}")
    private String saveImageDirectory;

    @Value("${upload.directory.crew.files}")
    private String saveFileDirectory;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    //@Autowired
    //private S3Service s3Service;


    @Autowired
    private CrewMapper crewMapper;

    @Autowired
    private UploadMapper uploadMapper;

    @Autowired
    private ReplyMapper replyMapper;


    public Crew addNewCrew(CrewForm form
            , @AuthenticationPrincipal LoginUser loginUser) {
        Crew crew = new Crew();
        crew.setNo(form.getNo());
        crew.setTitle(form.getTitle());
        crew.setName(form.getName());
        crew.setSchedule(form.getSchedule());
        crew.setLocation(form.getLocation());
        crew.setDescription(form.getDescription());

        MultipartFile image = form.getImage();
        MultipartFile upfile = form.getUpfile();

        // 작성자가 대표 이미지 추가 시, crews 테이블에 저장
        if (form.getImage() == null) {
            crew.setThumbnail(null);
        } else if (!image.isEmpty()) {
            String originalImageName = "crew" + form.getNo() + "_" + image.getOriginalFilename();
            String imageName = System.currentTimeMillis() + originalImageName;
            //s3Service.uploadFile(image, bucketName, saveImageDirectory, imageName);

            UploadFile uploadThumbnail = new UploadFile();
            uploadThumbnail.setOriginalName(originalImageName);
            uploadThumbnail.setSaveName(imageName);

            crew.setThumbnail(uploadThumbnail);
        }

        // 첨부파일 추가 시, crews 테이블에 저장
        if (form.getUpfile() == null) {
            crew.setUploadFile(null);
        } else if (!upfile.isEmpty()) {
            String originalFileName = upfile.getOriginalFilename();
            String filename = System.currentTimeMillis() + originalFileName;

            //s3Service.uploadFile(upfile, bucketName, saveFileDirectory, filename);

            UploadFile uploadFile = new UploadFile();
            uploadFile.setOriginalName(originalFileName);
            uploadFile.setSaveName(filename);

            crew.setUploadFile(uploadFile);
        }
        crewMapper.insertCrew(crew);

        // 썸네일/첨부파일 추가 시, crew_files 테이블에 저장
        if (crew.getThumbnail() != null) {
            UploadFile uploadThumbnail = crew.getThumbnail();
            uploadThumbnail.setNo(crew.getNo());
            uploadThumbnail.setSaveName(crew.getThumbnail().getSaveName());
            uploadThumbnail.setOriginalName(crew.getThumbnail().getOriginalName());
            uploadMapper.insertCrewFile(uploadThumbnail);
        }
        if (crew.getUploadFile() != null) {
            UploadFile uploadFile = crew.getUploadFile();
            uploadFile.setNo(crew.getNo());
            uploadFile.setSaveName(crew.getUploadFile().getSaveName());
            uploadFile.setOriginalName(crew.getUploadFile().getOriginalName());
            uploadMapper.insertCrewFile(uploadFile);
        }

        User user = new User();
        user.setNo(loginUser.getNo());
        user.setNickname(loginUser.getNickname());
        crew.setUser(user);

        // crew_members 테이블에 저장
        CrewMember member = new CrewMember();
        member.setCrewNo(crew.getNo());
        member.setReader("Y");
        member.setJoinDate(new Date());
        member.setJoin("Y");
        member.setUser(user);
        crewMapper.insertCrewMember(member);

        return crew;
    }

    public ListDto<Crew> getCrews(Map<String, Object> condition) {
        int totalRows = crewMapper.getTotalRowsForCrew(condition);

        int page = (Integer) condition.get("page");
        int rows = (Integer) condition.get("rows");
        Pagination pagination = new Pagination(page, totalRows, rows);

        condition.put("begin", pagination.getBegin());
        condition.put("end", pagination.getEnd());

        List<Crew> crews = crewMapper.getCrews(condition);
        ListDto<Crew> dto = new ListDto<>(crews, pagination);

        return dto;
    }

    // 가입 가능한 조회수 높은 크루 목록 반환
    public ListDto<Crew> getCrewsTop(Map<String, Object> condition) {
        List<Crew> crews = crewMapper.getCrewsTopFive(condition);
        ListDto<Crew> dto = new ListDto<>(crews);

        return dto;
    }

    public Crew getCrewDetail(int crewNo) {
        Crew crew = crewMapper.getCrewDetailByNo(crewNo);
        UploadFile uploadThumbnail = uploadMapper.getThumbnailByCrewNo(crewNo);
        UploadFile uploadFile = uploadMapper.getFileByCrewNo(crewNo);
        List<Reply> reply = replyMapper.getRepliesByTypeNo(crewNo);

        if (crew == null) {
            throw new CommunityException("존재하지 않는 게시글입니다.");
        }

        User user = new User();
        user.setNo(crew.getUser().getNo());
        user.setNickname(crew.getUser().getNickname());
        crew.setUser(user);

        crew.setThumbnail(uploadThumbnail);
        crew.setUploadFile(uploadFile);
        crew.setReply(reply);

        return crew;
    }

    public void updateCrewViewCnt(int crewNo) {
        Crew crew = crewMapper.getCrewDetailByNo(crewNo);
        crew.setViewCnt(crew.getViewCnt() + 1);
        crewMapper.updateCrewCnt(crew);
    }

    public Crew updateCrew(CrewForm form) {
        Crew savedCrew = crewMapper.getCrewDetailByNo(form.getNo());
        savedCrew.setNo(form.getNo());
        savedCrew.setType(form.getType());
        savedCrew.setTitle(form.getTitle());
        savedCrew.setLocation(form.getLocation());
        savedCrew.setDescription(form.getDescription());
        savedCrew.setSchedule(form.getSchedule());

        savedCrew.setName(form.getName());
        savedCrew.setDeleted("N");

        MultipartFile image = form.getImage();
        MultipartFile upfile = form.getUpfile();

        // 기존 썸네일 정보 조회
        UploadFile prevThumbnail = uploadMapper.getThumbnailByCrewNo(savedCrew.getNo());
        if (form.getImage() == null) {
            savedCrew.setThumbnail(null);
        } else if (!image.isEmpty()) {
            // 기존 썸네일이 존재하면 "Y"로 변경 저장
            if (prevThumbnail != null) {
                uploadMapper.updateCrewFile(prevThumbnail.getFileNo());
            }

            if (form.getImage() == null) {
                savedCrew.setThumbnail(prevThumbnail);
            } else {
                // 신규 썸네일 정보를 조회하여 CREW_FILES 테이블에 저장
                String originalImageName = "crew" + form.getNo() + "_" + image.getOriginalFilename();
                String ImageName = System.currentTimeMillis() + originalImageName;
                //s3Service.uploadFile(image, bucketName, saveImageDirectory, ImageName);

                UploadFile uploadThumbnail = new UploadFile();
                uploadThumbnail.setNo(savedCrew.getNo());
                uploadThumbnail.setOriginalName(originalImageName);
                uploadThumbnail.setSaveName(ImageName);
                savedCrew.setThumbnail(uploadThumbnail);
                uploadMapper.insertCrewFile(uploadThumbnail);
            }
        }

        // 기존 첨부파일 정보 조회
        UploadFile prevUploadFile = uploadMapper.getFileByCrewNo(savedCrew.getNo());
        if (form.getUpfile() == null) {
            savedCrew.setUploadFile(null);
        } else if (!upfile.isEmpty()) {
            // 기존 첨부파일이 존재하면 "Y"로 변경 저장
            if (prevUploadFile != null) {
                uploadMapper.updateCrewFile(prevUploadFile.getFileNo());
            }
            // 신규 첨부파일 정보를 조회하여 CREW_FILES 테이블에 저장
            String originalFileName = upfile.getOriginalFilename();
            String filename = System.currentTimeMillis() + originalFileName;

            //s3Service.uploadFile(upfile, bucketName, saveFileDirectory, filename);

            UploadFile uploadFile = new UploadFile();
            uploadFile.setNo(savedCrew.getNo());
            uploadFile.setOriginalName(originalFileName);
            uploadFile.setSaveName(filename);
            savedCrew.setUploadFile(uploadFile);

            uploadMapper.insertCrewFile(uploadFile);
        }

        if (prevUploadFile != null) {
            UploadFile uploadFile = new UploadFile();
            uploadFile.setNo(savedCrew.getNo());
            uploadFile.setOriginalName(prevUploadFile.getOriginalName());
            uploadFile.setSaveName(prevUploadFile.getSaveName());
            savedCrew.setUploadFile(uploadFile);
        }

        crewMapper.updateCrew(savedCrew);

        return savedCrew;
    }

    public void deleteCrew(int crewNo) {
        Crew savedCrew = crewMapper.getCrewDetailByNo(crewNo);
        savedCrew.setDeleted("Y");

        crewMapper.updateCrew(savedCrew);
    }

    public void deleteCrewFile(int fileNo) {
        uploadMapper.updateCrewFile(fileNo);
    }

    public boolean isExistCrewMember(int crewNo, @AuthenticationPrincipal LoginUser loginUser) {
        List<Integer> userNoList = crewMapper.getCrewMembers(crewNo);

        boolean isExists = false;

        for (int userNo: userNoList) {
            if (userNo == loginUser.getNo()) {
                isExists = true;
                break;
            }
        }

        return isExists;
    }

    public int getEnterMemberCnt(int crewNo) {
        return crewMapper.getCrewMemberCnt(crewNo);
    }

    public void updateCrewCondition(int crewNo, String condition) {
        crewMapper.updateCrewCondition(crewNo, condition);
    }

    public void enterCrew(int crewNo
            , @AuthenticationPrincipal LoginUser loginUser) {
        CrewMember member = new CrewMember();
        member.setCrewNo(crewNo);
        member.setReader("N");
        member.setJoin("Y");
        member.setJoinDate(new Date());

        User user = new User();
        user.setNo(loginUser.getNo());
        member.setUser(user);

        crewMapper.insertCrewMember(member);
    }

    public void leaveCrew(int crewNo
            , @AuthenticationPrincipal LoginUser loginUser) {
        CrewMember member = new CrewMember();
        member.setCrewNo(crewNo);
        member.setJoin("N");

        User user = new User();
        user.setNo(loginUser.getNo());
        member.setUser(user);

        crewMapper.updateCrewMember(member);
    }

    public List<Crew> getCrewByUserNo(int userNo) {
        return crewMapper.getCrewByUserNo(userNo);
    }

    public List<CrewMember> getCrewMembersByCrewId(int crewNo){

        List<CrewMember> members = crewMapper.getByCrewNo(crewNo);

        List<CrewMember> availableMembers = new ArrayList<>();

        for (CrewMember member : members) {
            // 'Y'가 아닌 reader 값을 가진 멤버만 추가
            if (!"Y".equals(member.getReader())){
                availableMembers.add(member);
            }
        }

        //필터링된 멤버 목록 반환
        return availableMembers;
    }

    public void updateReader(int userNo, int crewNo, int readerNo){
        // 셀렉트 박스에서 유저의 no를 활용해서 update를 시키기 위한 updateReader 메소드
        crewMapper.updateReader(userNo, crewNo);
        // 위임이라는 기능을 사용하기 위해서는 리더의 계정 로그인을 필수로 해야해서 user.getNo를 통해서 리더의 번호를 넘겨줌
        crewMapper.exitCrew(readerNo, crewNo);
    }
}