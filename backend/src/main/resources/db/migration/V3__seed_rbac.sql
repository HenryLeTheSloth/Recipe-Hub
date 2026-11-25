insert into permissions (code, description) values
    ('RECIPE_WRITE',          'Viết và quản lý công thức của mình'),
    ('COMMUNITY_PARTICIPATE', 'Đánh giá, bình luận, lưu yêu thích'),
    ('AI_BOWL_USE',           'Dùng Cái bát AI'),
    ('MEDIA_UPLOAD',          'Tải ảnh lên'),
    ('CONTENT_MODERATE',      'Duyệt, ẩn/hiện và xóa nội dung'),
    ('CATALOG_MANAGE',        'Quản lý danh mục, thẻ, nguyên liệu'),
    ('USER_READ',             'Xem danh sách người dùng'),
    ('USER_MANAGE',           'Khóa, xóa, gán vai trò người dùng'),
    ('ROLE_MANAGE',           'Quản lý vai trò và quyền');

insert into roles (code, name, description, system) values
    ('ADMIN',     'Quản trị viên', 'Toàn quyền hệ thống',            true),
    ('MODERATOR', 'Kiểm duyệt viên', 'Duyệt nội dung và quản lý danh mục', true),
    ('MEMBER',    'Thành viên',    'Người dùng đã đăng ký',          true);

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on
    r.code = 'ADMIN'
    or (r.code = 'MODERATOR' and p.code in ('RECIPE_WRITE', 'COMMUNITY_PARTICIPATE', 'AI_BOWL_USE', 'MEDIA_UPLOAD',
                                            'CONTENT_MODERATE', 'CATALOG_MANAGE', 'USER_READ'))
    or (r.code = 'MEMBER' and p.code in ('RECIPE_WRITE', 'COMMUNITY_PARTICIPATE', 'AI_BOWL_USE', 'MEDIA_UPLOAD'));
