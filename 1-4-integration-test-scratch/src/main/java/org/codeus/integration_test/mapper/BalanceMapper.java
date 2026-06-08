package org.codeus.integration_test.mapper;

import org.codeus.integration_test.dto.response.BalanceResponse;
import org.codeus.integration_test.model.UserBalance;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BalanceMapper {

    BalanceResponse toResponse(UserBalance userBalance);

    List<BalanceResponse> toResponseList(List<UserBalance> userBalances);
}
