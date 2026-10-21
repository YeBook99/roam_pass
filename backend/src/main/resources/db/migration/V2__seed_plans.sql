-- 시드 요금제 (가격은 원)
INSERT INTO sim_plan (plan_code, country, days, data_mb, sim_type, price) VALUES
    ('ESIM-JP-3D-3GB',   'JP', 3,  3072,  'ESIM',  9900),
    ('ESIM-JP-7D-10GB',  'JP', 7,  10240, 'ESIM', 19900),
    ('ESIM-US-7D-5GB',   'US', 7,  5120,  'ESIM', 17900),
    ('ESIM-VN-5D-5GB',   'VN', 5,  5120,  'ESIM',  8900),
    ('ESIM-EU-10D-10GB', 'EU', 10, 10240, 'ESIM', 29900),
    ('USIM-JP-5D-5GB',   'JP', 5,  5120,  'USIM', 14900),
    ('USIM-US-10D-10GB', 'US', 10, 10240, 'USIM', 27900),
    ('USIM-VN-7D-7GB',   'VN', 7,  7168,  'USIM', 11900);
