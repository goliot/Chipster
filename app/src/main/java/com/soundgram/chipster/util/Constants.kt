package com.soundgram.chipster.util

object Constants {

    const val ERROR = "[ERROR]"
    const val PERMISSION_REQUEST_CODE = 1001
    const val CAMERA_PERMISSION_REQUEST_CODE = 1002 // 권한 요청 코드
    const val GALLERY_REQUEST_CODE = 2001
    const val CAMERA_REQUEST_CODE = 2002
    const val CROP_REQUEST_CODE = 2003

    const val IMAGE_URI = "imageURI"

    // IMAGE_PATH.format(팩아이디, 이미지이름) 으로 입력할 것
    const val IMAGE_PATH =
        "https://chipsterplay.soundgram.co.kr/media/arpoca/%d/pack_img/%s"
}