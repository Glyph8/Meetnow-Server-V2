package timetogeter.context.place.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import timetogeter.context.auth.exception.UserNotFoundException;
import timetogeter.context.place.application.dto.ConfirmedPlaceDTO;
import timetogeter.context.place.application.dto.PlaceDTO;
import timetogeter.context.place.application.dto.response.PlaceBoardDTO;
import timetogeter.context.place.domain.entity.PlaceBoard;
import timetogeter.context.place.domain.entity.PromisePlace;
import timetogeter.context.place.domain.entity.UserBoard;
import timetogeter.context.place.domain.repository.PlaceBoardRepository;
import timetogeter.context.place.domain.repository.PromisePlaceRepository;
import timetogeter.context.place.domain.repository.UserBoardRepository;
import timetogeter.context.place.exception.PlaceNotFoundException;
import timetogeter.context.promise.application.dto.response.PromiseRegisterDTO;
import timetogeter.context.promise.application.service.PromiseConfirmService;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;

import java.util.List;
import java.util.stream.Collectors;

import static timetogeter.global.common.util.PageUtil.PLACE_PAGE;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceBoardService { // TODO: 장소 관리 시스템

    private final PromisePlaceRepository placeRepository;
    private final PlaceBoardRepository placeBoardRepository;
    private final VotingService votingService;
    private final PromiseConfirmService promiseConfirmService;

    public PlaceBoardDTO getPlaceBoard(String userId, String promiseId, int page) {
        if(page < 1) throw new PlaceNotFoundException(BaseErrorCode.PAGE_NOT_FOUND, "[ERROR] 총 페이지를 초과한 요청입니다.");
        page = page-1;

        PageRequest pageRequest = PageRequest.of(page, PLACE_PAGE);
        Page<PromisePlace> placePage = placeRepository.findByPromiseId(promiseId, pageRequest);

        if(page > placePage.getTotalPages()) throw new PlaceNotFoundException(BaseErrorCode.PAGE_NOT_FOUND, "[ERROR] 총 페이지를 초과한 요청입니다.");

        List<PlaceDTO> places = placePage.getContent()
                .stream()
                .map(p -> new PlaceDTO(p.getPlaceId(), p.getPlaceName(), p.getPlaceAddr(), p.getVoting(),
                        p.hasVotedBy(userId),  // 장소 삭제 유무
                        votingService.hasVotedBy(userId, p.getPlaceId()), p.getAiPlaceId())) // 투표 취소가 가능한지
                .collect(Collectors.toList());

        return new PlaceBoardDTO(page, placePage.getTotalPages(), places);
    }

    @Transactional
    public void vote(String userId, String promiseId, int placeId) {
        PromisePlace place = get(placeId);
        int voteNum = place.getVoting();
        promiseConfirmService.checkTotalVote(promiseId, voteNum);

        votingService.vote(userId, placeId);
        place.vote();
        placeRepository.save(place);
    }

    public void deleteVote(String userId, int placeId) {
        votingService.cancelVote(userId, placeId);

        PromisePlace place = get(placeId);
        place.cancelVote();
        placeRepository.save(place);
    }

    @Transactional
    public PromiseRegisterDTO confirmedPlace(String userId, String promiseId, int placeId, Integer aiPlaceId) {
        boolean isConfirmed = promiseConfirmService.confirmedPromiseManager(userId, promiseId);
        if(!isConfirmed) throw new UserNotFoundException(BaseErrorCode.PROMISE_MANGER_FORBIDDEN, "[ERROR] 사용자에게 약속장 권한이 없습니다.");

        if(aiPlaceId == -1 || aiPlaceId == 0) {
            PromisePlace promisePlace = get(placeId);
            if (!promisePlace.getAiPlace()) {
                PlaceBoard placeBoard = PlaceBoard.of(promisePlace.getPlaceName(), promisePlace.getPlaceAddr(),
                        promisePlace.getAiPlace(), promisePlace.getPlaceInfo());
                placeBoardRepository.save(placeBoard);
                placeId = placeBoard.getPlaceBoardId();
            }
        }else{
            placeId = aiPlaceId;
        }
        promiseConfirmService.confirmPromisePlace(promiseId, placeId);
        return promiseConfirmService.confirmedScheduleByPlace(promiseId, placeId);
    }

    private PromisePlace get(int placeId){
        PromisePlace place = placeRepository.findByPlaceId(placeId)
                .orElseThrow(() -> new PlaceNotFoundException(BaseErrorCode.PLACE_NOT_FOUND, "[ERROR] 아이디에 해당하는 장소를 찾을 수 없습니다."));
        return place;
    }

    public ConfirmedPlaceDTO confirmedPlaceCheck(String promiseId) {
        int placeId = promiseConfirmService.confirmedPlaceCheck(promiseId);
        if(placeId == -1)
            throw new PlaceNotFoundException(BaseErrorCode.PLACE_NOT_CONFIRM, "[ERROR] 약속에 해당하는 장소가 확정되지 않았습니다.");

        PlaceBoard placeBoard = placeBoardRepository.findById(placeId).orElseThrow(() -> new PlaceNotFoundException(BaseErrorCode.PLACE_NOT_FOUND, "[ERROR] 아이디에 해당하는 장소를 찾을 수 없습니다."));
        return new ConfirmedPlaceDTO(placeBoard.getPlaceBoardId(), placeBoard.getPlaceName(), placeBoard.getPlaceAddr());
    }
}
