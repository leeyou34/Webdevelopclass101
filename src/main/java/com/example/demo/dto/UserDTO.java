package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*===============================================================
*Dec 30th 2021, 
*Authenticated Backend implementation.
*
* 실습코드 4-4. UserDTO.java 
*================================================================
*/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
	private String token;
	private String email;
	private String username;
	private String password;
	private String id;
	
}
