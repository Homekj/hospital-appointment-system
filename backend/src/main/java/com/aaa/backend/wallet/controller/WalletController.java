package com.aaa.backend.wallet.controller;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.wallet.dto.RechargeRequest;
import com.aaa.backend.wallet.service.WalletService;
import com.aaa.backend.wallet.vo.BalanceLogVO;
import com.aaa.backend.wallet.vo.RechargeResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserContext currentUserContext;

    public WalletController(WalletService walletService, CurrentUserContext currentUserContext) {
        this.walletService = walletService;
        this.currentUserContext = currentUserContext;
    }

    @PostMapping("/recharge")
    public ApiResponse<RechargeResultVO> recharge(@Valid @RequestBody RechargeRequest body) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(walletService.recharge(userId, body));
    }

    @GetMapping("/logs")
    public ApiResponse<PageResult<BalanceLogVO>> logs(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "current 必须大于或等于 1") long current,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size 必须大于或等于 1")
            @Max(value = 100, message = "size 不能超过 100") int size) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(walletService.logs(userId, current, size));
    }
}
