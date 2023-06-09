package com.soundgram.chipster.util

object Constants {

    const val ERROR = "[ERROR]"
    const val PERMISSION_REQUEST_CODE = 1001
    const val CAMERA_PERMISSION_REQUEST_CODE = 1002 // 권한 요청 코드
    const val GALLERY_REQUEST_CODE = 2001
    const val CAMERA_REQUEST_CODE = 2002
    const val CROP_REQUEST_CODE = 2003
    const val MOVE_MAIN = 2004
    const val MOVE_BINDER = 2005
    const val MOVE_MAP = 2006
    const val MOVE_DETAIL = 2007


    const val IMAGE_URI = "imageURI"
    const val PACK_ID = "packId"
    const val USER_ID = "userId"

    const val DEFAULT_USER_ID = 1025
    const val DEFAULT_PACK_ID = 307

    // IMAGE_PATH.format(팩아이디, 이미지이름) 으로 입력할 것
    const val IMAGE_PATH =
        "https://chipsterplay.soundgram.co.kr/media/arpoca/%d/pack_img/%s"
}