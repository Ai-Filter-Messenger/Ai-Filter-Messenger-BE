CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- 유저 고유 ID
    username VARCHAR(50) NOT NULL,      -- 유저 이름
    email VARCHAR(100) NOT NULL,        -- 유저 이메일
    password VARCHAR(255) NOT NULL,     -- 유저 비밀번호 (암호화된 값)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- 계정 생성 시각
);

CREATE TABLE files (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- 파일 고유 ID
    user_id INT,                        -- 파일을 업로드한 유저 ID (users 테이블의 외래 키)
    filename VARCHAR(255) NOT NULL,     -- 파일 이름
    path VARCHAR(255) NOT NULL,         -- 파일이 저장된 경로
    mimetype VARCHAR(100),              -- 파일의 MIME 타입
    size INT,                           -- 파일 크기 (바이트 단위)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 파일 업로드 시각
    FOREIGN KEY (user_id) REFERENCES users(id)       -- users 테이블의 id를 참조하는 외래 키
);